# Pelican on the SOE box: The Lost Centuries build server

A 1.21.1 NeoForge server for building templates with a friend, running next to SOE on the
OVH box (Ryzen 7 9700X, 64 GB). Pelican Panel + Wings (v1.0 release candidates as of Nov 2026).

## Layout

```
            Tailscale only (HTTP inside the tailnet)            Public
  browser ──> panel  100.x:8090  (Docker, this compose.yml)
  browser ──> wings  100.x:8080  (systemd, console websocket)
  sftp    ──> wings  100.x:2022
                                                        friend ──> minecraft :25565
```

The panel, Wings API and SFTP listen **only on the Tailscale IP**, so there's no public admin surface and no
certificates to manage. Only the Minecraft port is public. Your friend only needs the server address and the pack.

Files here:

| File | |
|---|---|
| `preflight.sh` | read-only checks: kernel, ports, Docker networks, memory |
| `compose.yml`, `.env.example` | Pelican Panel container |
| `wings.service` | systemd unit for Wings |
| `egg-the-lost-centuries.json` | server template: NeoForge 21.1.265 + packwiz sync on every start |

## 1. Preflight

```sh
scp -r server/pelican soe:/opt/pelican     # or git clone on the box
ssh soe 'bash /opt/pelican/preflight.sh'
```

Check for:
- **Ports** 8090, 8080, 2022, 25565 free (SOE uses 4242).
- **Docker subnets**: the panel uses `172.20.0.0/16` and Wings `172.18.0.0/16`. If SOE's compose networks
  already use either, change `PANEL_SUBNET`/`PANEL_GATEWAY` in `.env`, and/or Wings' `docker.network` (step 3).
- **Kernel**: SOE already runs in Docker, so the OVH kernel is fine, but the script flags OVH `-grs` kernels.

## 2. Panel

```sh
cd /opt/pelican
cp .env.example .env && $EDITOR .env        # TS_IP=$(tailscale ip -4), PANEL_HOST, ADMIN_EMAIL
docker compose up -d
docker compose logs panel | grep 'Generated app key:'   # save this key in KeePassXC
```

Open `http://<TS_IP>:8090/installer` and keep the defaults (SQLite, filesystem cache/session, database queue).
Create your admin user there. The first start runs migrations and can take a few minutes.

## 3. Wings

```sh
mkdir -p /etc/pelican /var/run/wings
curl -L -o /usr/local/bin/wings https://github.com/pelican/wings/releases/latest/download/wings_linux_amd64
chmod u+x /usr/local/bin/wings
cp /opt/pelican/wings.service /etc/systemd/system/wings.service
```

In the panel: **Admin → Nodes → Create**
- FQDN: the Tailscale IP (same as `TS_IP`). SSL: **off / HTTP**. Daemon port 8080, SFTP port 2022.
- Memory: about 16384 MB to start (Minecraft only; SOE runs outside Pelican). Disk: about 50 GB.

Copy the node's **Configuration** tab into `/etc/pelican/config.yml`, then edit these keys:

```yaml
api:
  host: 100.x.y.z          # Tailscale IP, so the API isn't public
system:
  sftp:
    bind_address: 100.x.y.z
docker:
  # only if preflight showed 172.18.x already in use:
  # network:
  #   interface: 172.19.0.1
  #   interfaces:
  #     v4:
  #       subnet: 172.19.0.0/16
  #       gateway: 172.19.0.1
  installer_limits:
    memory: 4096           # NeoForge's installer needs more than the 1024 MB default
```

```sh
wings --debug              # first run pulls images; Ctrl+C once it says it's running
systemctl daemon-reload && systemctl enable --now wings
```

The node should show a green heart in the panel.

## 4. Allocation and firewall

Node → **Allocations**: add IP `0.0.0.0` (or the box's public IP), port **25565**.

Docker-published ports bypass `ufw`. If the OVH Network Firewall is enabled, allow 25565/tcp there.
Optional: add a DNS record, e.g. `mc.skiesofeden.net` → box IP.

## 5. Egg

Admin → **Eggs → Import** → `egg-the-lost-centuries.json`. It's the pelican-eggs NeoForge egg, pinned to
MC 1.21.1 / NeoForge 21.1.265 and Java 21, with two extra variables:

| Variable | |
|---|---|
| `PACK_URL` | the pack's `pack.toml` URL, reachable without auth |
| `PACK_SYNC` | `1` (default) = run `packwiz-installer -s server` before every start |

Startup command: `packwiz-installer-bootstrap.jar -g -s server "$PACK_URL"`, then the normal NeoForge start.
A failed sync (e.g. GitHub down) logs a warning and starts with the existing files.

**`PACK_URL` must be reachable without auth.**
- **Public repo (current):** `https://raw.githubusercontent.com/garretreichenbach/TheLostCenturies/main/pack/pack.toml` (the egg's default)
- **Private repo:** packwiz-installer can't send auth headers. You'd need to publish just the `pack/` folder
  somewhere public (GitHub Pages from a separate repo, or the box's existing web server).

## 6. Server

Create server: owner you, egg *The Lost Centuries*, image Java 21.
- Memory **10240 MB** (about 55 mods incl. Create + MineColonies; 8 GB minimum). CPU unlimited or 400%, disk 20 GB.
- `PACK_URL` as above.

Install, accept the EULA, start. The first start downloads the server-side mods (client-only mods like Sodium,
Iris and the maps are skipped), generates the Lost Cities world, and should log
`Dry zone applied to the overworld`.

**Companion mod:** `lostcenturies-*.jar` isn't published yet, so upload it via the file manager or SFTP
(`sftp -P 2022 <panel-user>.<server-id>@100.x.y.z`) into `mods/`. packwiz-installer leaves files it didn't
install alone. Long term: attach it to a GitHub release and `packwiz url add` it.

### For building sessions

```
whitelist on
whitelist add <friend>
op <you>          (and the friend, if they should use creative / build tools)
lostcenturies preset easy
```

Plus `gamemode creative <player>` while building. Back up before experimenting:
Pelican **Backups** (stored in `/var/lib/pelican/backups`) and a daily **Schedule** with a backup task.

## 7. Your friend's client

Same as your Prism setup: a 1.21.1 NeoForge instance with packwiz-installer-bootstrap as the pre-launch command,
pointing at the same `PACK_URL`. Alternatively send them an export: `cd pack && packwiz mr export`.
The companion mod has no registries or network channels, so clients can join without it.

## Updating

Edit the pack, `packwiz refresh`, commit and push, then restart the server: it syncs on start.

## Memory budget (64 GB box)

SOE plus its JVM stays as it is; Minecraft 10 GB (+ ~1 GB container overhead); panel ~300 MB; Wings ~100 MB.
Check SOE's actual usage in preflight's `docker stats` output before going bigger.
