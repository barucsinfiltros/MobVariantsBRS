$client = New-Object System.Net.Sockets.TcpClient('127.0.0.1',25575)
$stream = $client.GetStream()
$password = [System.Text.Encoding]::ASCII.GetBytes('mvbrs_rcon')
$reqId = [System.BitConverter]::GetBytes([int]1)
[Array]::Reverse($reqId)
$pad = [byte[]](0,0)
$packet = [System.BitConverter]::GetBytes([int]($password.Length + 2)) + $reqId + $pad + $password + $pad
[Array]::Reverse($packet[0..3])
$stream.Write($packet,0,$packet.Length)
$stream.Flush()
Start-Sleep -Milliseconds 500
$buf = New-Object byte[] 4096
$read = $stream.Read($buf,0,$buf.Length)
$authResp = [System.Text.Encoding]::ASCII.GetString($buf,0,$read)
Write-Host "Auth response: $authResp"
$command = [System.Text.Encoding]::ASCII.GetBytes('list')
$reqId2 = [System.BitConverter]::GetBytes([int]2)
[Array]::Reverse($reqId2)
$packet2 = [System.BitConverter]::GetBytes([int]($command.Length + 2)) + $reqId2 + $pad + $command + $pad
[Array]::Reverse($packet2[0..3])
$stream.Write($packet2,0,$packet2.Length)
$stream.Flush()
Start-Sleep -Milliseconds 500
$buf2 = New-Object byte[] 4096
$read2 = $stream.Read($buf2,0,$buf2.Length)
$cmdResp = [System.Text.Encoding]::ASCII.GetString($buf2,0,$read2)
Write-Host "Command response: $cmdResp"
$stream.Close()
$client.Close()
