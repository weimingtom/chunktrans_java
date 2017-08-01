@luac  -o return.luac return.lua
@lua ChunkSpy.lua return.luac return.lua -o return.luac.txt
@luac -o hello.luac hello.lua 
@lua ChunkSpy.lua hello.luac hello.lua -o hello.luac.txt
@luac -o fib.luac fib.lua
@lua ChunkSpy.lua fib.luac fib.lua -o fib.luac.txt
@pause
