#### 视频链接

- https://www.bilibili.com/video/BV1py4y1E7oA/?p=91

#### 该部分内容

- [ ] 黏包、半包问题
- [x] 协议设计与解析


#### Redis简易客户端执行日志

```text
20:32:26.558 [DEBUG] [main] c.c.j.n.p.TestRedisClient - 连接Redis服务器中...
20:32:26.567 [DEBUG] [nioEventLoopGroup-2-1] i.n.h.l.LoggingHandler - [id: 0xd8fd92b7] REGISTERED
20:32:26.567 [DEBUG] [nioEventLoopGroup-2-1] i.n.h.l.LoggingHandler - [id: 0xd8fd92b7] CONNECT: localhost/127.0.0.1:6379
20:32:26.568 [DEBUG] [main] c.c.j.n.p.TestRedisClient - 连接成功
20:32:26.569 [DEBUG] [nioEventLoopGroup-2-1] i.n.h.l.LoggingHandler - [id: 0xd8fd92b7, L:/127.0.0.1:4346 - R:localhost/127.0.0.1:6379] ACTIVE
20:32:26.577 [DEBUG] [nioEventLoopGroup-2-1] c.c.j.n.p.TestRedisClient - 命令发送: *3
$3
set
$4
name
$4
jack

20:32:26.578 [DEBUG] [nioEventLoopGroup-2-1] i.n.h.l.LoggingHandler - [id: 0xd8fd92b7, L:/127.0.0.1:4346 - R:localhost/127.0.0.1:6379] WRITE: 33B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 2a 33 0d 0a 24 33 0d 0a 73 65 74 0d 0a 24 34 0d |*3..$3..set..$4.|
|00000010| 0a 6e 61 6d 65 0d 0a 24 34 0d 0a 6a 61 63 6b 0d |.name..$4..jack.|
|00000020| 0a                                              |.               |
+--------+-------------------------------------------------+----------------+
20:32:26.578 [DEBUG] [nioEventLoopGroup-2-1] i.n.h.l.LoggingHandler - [id: 0xd8fd92b7, L:/127.0.0.1:4346 - R:localhost/127.0.0.1:6379] FLUSH
20:32:26.580 [DEBUG] [nioEventLoopGroup-2-1] i.n.h.l.LoggingHandler - [id: 0xd8fd92b7, L:/127.0.0.1:4346 - R:localhost/127.0.0.1:6379] READ: 5B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 2b 4f 4b 0d 0a                                  |+OK..           |
+--------+-------------------------------------------------+----------------+
20:32:26.580 [DEBUG] [nioEventLoopGroup-2-1] c.c.j.n.p.TestRedisClient - Redis响应: +OK

20:32:26.580 [DEBUG] [nioEventLoopGroup-2-1] i.n.h.l.LoggingHandler - [id: 0xd8fd92b7, L:/127.0.0.1:4346 - R:localhost/127.0.0.1:6379] READ COMPLETE
```