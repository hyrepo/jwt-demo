# 什么是JWT

JWT(JSON Web Token)是一个开放标准（RFC 7519），它定义了一种紧凑且独立的方式，可以在各个系统之间用JSON作为对象安全地传输信息，并且可以保证所传输的信息不会被篡改。

JWT通常有两种应用场景：

- 授权。这是最常见的JWT使用场景。一旦用户登录，每个后续请求将包含一个JWT，作为该用户访问资源的令牌。这也是此文章讨论的内容。
- 信息交换。可以利用JWT在各个系统之间安全地传输信息，JWT的特性使得接收方可以验证收到的内容是否被篡改。

# JWT的结构


![](https://www.cnblogs.com/images/cnblogs_com/xz816111/786501/o_jwt.png)

JWT由三部分组成，用`.`分割开。

## Header

第一部分为`Header`，通常由两部分组成：令牌的类型，即JWT，以及所使用的加密算法。

```
{
  "alg": "HS256",
  "typ": "JWT"
}
```

`Base64`加密后，就变成了:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
```


## Payload
第二部分为`Payload`，里面可以放置自定义的信息，以及过期时间、发行人等。

```
{
  "sub": "1234567890",
  "name": "John Doe",
  "iat": 1516239022
}
```

`Base64`加密后，就变成了:
```
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ
```

## Signature

第三部分为`Signature`，计算此签名需要四部分信息：

- `Header`里的算法信息
- `Header`
- `Payload`
- 一个自定义的秘钥

接受到JWT后，利用相同的信息再计算一次签名，然年与JWT中的签名对比，如果不相同则说明JWT中的内容被篡改。

## 解码后的JWT

![](https://www.cnblogs.com/images/cnblogs_com/xz816111/786501/o_jwt-decode.png)

将上面三部分都编码后再合在一起就得到了JWT。

需要注意的是，**JWT的内容并不是加密的，只是简单的`Base64`编码。**也就是说，JWT一旦泄露，里面的信息可以被轻松获取，因此不应该用JWT保存任何敏感信息。


# JWT是怎样工作的

![](https://www.cnblogs.com/images/cnblogs_com/xz816111/786501/o_jwt-work.png)

1. 应用程序或客户端向授权服务器请求授权。这里的授权服务器可以是单独的一个应用，也可以和API集成在同一个应用里。
2. 授权服务器向应用程序返回一个JWT。
3. 应用程序将JWT放入到请求里（通常放在`HTTP`的`Authorization`头里）
4. 服务端接收到请求后，验证JWT并执行对应逻辑。

# 在JAVA里使用JWT


## 引入依赖

```
<dependency>
	<groupId>io.jsonwebtoken</groupId>
	<artifactId>jjwt</artifactId>
</dependency>
```

这里使用了一个叫JJWT(Java JWT)的库。

## JWT Service

### 生成JWT

```
public String generateToken(String payload) {
        return Jwts.builder()
                .setSubject(payload)
                .setExpiration(new Date(System.currentTimeMillis() + 10000))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
```

- 这里设置过期时间为10秒，因此生成的JWT只在10秒内能通过验证。
- 需要提供一个自定义的秘钥。

### 解码JWT

```
public String parseToken(String jwt) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(jwt)
                .getBody()
                .getSubject();
    }
```

- 解码时会检查JWT的签名，因此需要提供秘钥。

### 验证JWT

```
public boolean isTokenValid(String jwt) {
        try {
            parseToken(jwt);
        } catch (Throwable e) {
            return false;
        }
        return true;
    }
```

- `JJWT`并没有提供判断JWT是否合法的方法，但是在解码非法JWT时会抛出异常，因此可以通过捕获异常的方式来判断是否合法。

## 注册/登录

```
@GetMapping("/registration")
    public String register(@RequestParam String username, HttpServletResponse response) {
        String jwt = jwtService.generateToken(username);
        response.setHeader(JWT_HEADER_NAME, jwt);

        return String.format("JWT for %s :\n%s", username, jwt);
    }
```

- 需要为还没有获取到JWT的用户提供一个这样的注册或者登录入口，来获取JWT。
- 获取到响应里的JWT后，要在后续的请求里包含JWT，这里放在请求的`Authorization`头里。

## 验证JWT

```
@Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String jwt = httpServletRequest.getHeader(JWT_HEADER_NAME);
        if (WHITE_LIST.contains(httpServletRequest.getRequestURI())) {
            chain.doFilter(request, response);
        } else if (isTokenValid(jwt)) {
            updateToken(httpServletResponse, jwt);
            chain.doFilter(request, response);
        } else {
            httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
    
private void updateToken(HttpServletResponse httpServletResponse, String jwt) {
        String payload = jwtService.parseToken(jwt);
        String newToken = jwtService.generateToken(payload);
        httpServletResponse.setHeader(JWT_HEADER_NAME, newToken);
    }
```

- 将验证操作放在`Filter`里，这样除了登录入口，其它的业务代码将感觉不到JWT的存在。
- 将登录入口放在`WHITE_LIST`里，跳过对这些入口的验证。
- **需要刷新JWT**。如果JWT是合法的，那么应该用同样的`Payload`来生成一个新的JWT，这样新的JWT就会有新的过期时间，用此操作来刷新JWT，以防过期。
- 如果使用`Filter`，那么刷新的操作要在调用`doFilter()`之前，因为调用之后就无法再修改`response`了。

## API

```
private final static String JWT_HEADER_NAME = "Authorization";

    @GetMapping("/api")
    public String testApi(HttpServletRequest request, HttpServletResponse response) {
        String oldJwt = request.getHeader(JWT_HEADER_NAME);
        String newJwt = response.getHeader(JWT_HEADER_NAME);

        return String.format("Your old JWT is:\n%s \nYour new JWT is:\n%s\n", oldJwt, newJwt);
    }
```

这时候API就处于JWT的保护下了。API可以完全不用感知到JWT的存在，同时也可以主动获取JWT并解码，以得到JWT里的信息。如上所示。


# 尾注

- 完整的DEMO可以在这里找到：https://github.com/Beginner258/jwt-demo
- 参考资料：https://jwt.io/