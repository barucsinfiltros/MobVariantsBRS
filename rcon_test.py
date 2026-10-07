from mcrcon import MCRcon

with MCRcon('127.0.0.1', 'mvbrs_rcon', 25575) as mcr:
    resp = mcr.command('list')
    print(resp)
