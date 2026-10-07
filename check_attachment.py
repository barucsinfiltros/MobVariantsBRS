from mcrcon import MCRcon

mcr = MCRcon('127.0.0.1', 'mvbrs_rcon', 25575)
mcr.connect()
print(mcr.command('summon zombie ~ ~1 ~'))
print(mcr.command('list'))
print(mcr.command('data get entity @e[type=zombie,limit=1]'))
mcr.disconnect()