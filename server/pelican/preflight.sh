#!/usr/bin/env bash
# Read-only checks before installing Pelican next to SOE. Run on the SOE box: bash preflight.sh
set -u
hr() { printf '\n== %s\n' "$1"; }

hr "System"
uname -r
echo "virt: $(systemd-detect-virt 2>/dev/null || echo unknown)   (kvm/none = fine; openvz/lxc = Wings won't work)"
case "$(uname -r)" in *-grs-ipv6-64|*-mod-std-ipv6-64) echo "WARNING: OVH custom kernel; Wings docs say these are often unsupported";; esac

hr "Memory / disk"
free -h
df -h / /var/lib 2>/dev/null | sort -u

hr "Docker"
docker --version
docker compose version 2>/dev/null || echo "WARNING: docker compose v2 plugin missing"

hr "Tailscale"
tailscale ip -4 2>/dev/null || echo "WARNING: tailscale not found / not up"

hr "Ports already listening (need free: 8090 panel, 8080 wings, 2022 sftp, 25565 minecraft)"
for p in 8090 8080 2022 25565; do
  if ss -ltnH "( sport = :$p )" | grep -q .; then echo "  $p IN USE: $(ss -ltnpH "( sport = :$p )" | head -1)"; else echo "  $p free"; fi
done

hr "Docker networks and subnets (panel uses 172.20.0.0/16, Wings defaults to 172.18.0.0/16 unless changed)"
for n in $(docker network ls -q); do
  docker network inspect -f '{{.Name}}  {{range .IPAM.Config}}{{.Subnet}} {{end}}' "$n"
done
docker network inspect -f '{{range .IPAM.Config}}{{.Subnet}}{{"\n"}}{{end}}' $(docker network ls -q) 2>/dev/null \
  | grep -E '^172\.(18|20)\.' && echo "WARNING: overlap with a Pelican default subnet; change PANEL_SUBNET or Wings' docker.network settings"

hr "Running containers (memory)"
docker stats --no-stream --format 'table {{.Name}}\t{{.MemUsage}}' 2>/dev/null | head -20
