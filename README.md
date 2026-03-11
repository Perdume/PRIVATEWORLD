# PrivateWorld

> **Minecraft Bukkit/Spigot 플러그인** — 플레이어가 자신만의 개인 월드를 생성하고, 설정을 자유롭게 커스터마이징하며, 워크샵을 통해 파쿠르·PvP 등 다양한 게임 콘텐츠를 서버 전체에 공유할 수 있게 해주는 플러그인입니다.

---

## 목차
- [주요 기능](#주요-기능)
- [명령어](#명령어)
  - [일반 명령어](#일반-명령어)
  - [관리자 명령어](#관리자-명령어)
- [월드 옵션](#월드-옵션)
- [워크샵 (콘텐츠 공유)](#워크샵-콘텐츠-공유)
- [설치 방법](#설치-방법)
- [설정 파일](#설정-파일)
- [패키지 구조](#패키지-구조)
- [의존성](#의존성)

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| **개인 월드 생성** | 플레이어마다 최대 N개의 개인 Void 월드를 생성할 수 있습니다 |
| **월드 옵션 설정** | PvP, 블록 파괴/설치, 레드스톤, 날씨, 시간 고정 등 세밀한 설정 지원 |
| **방문 시스템** | 다른 플레이어의 월드를 GUI로 탐색하고 방문할 수 있습니다 |
| **밴/언밴** | 특정 플레이어를 자신의 월드에서 추방/복구할 수 있습니다 |
| **워크샵** | 자신의 월드를 파쿠르·PvP·어드벤처 등의 카테고리로 공개 등록해 다른 플레이어가 참가할 수 있게 합니다 |
| **설정 프리셋** | 월드 옵션 조합을 프리셋으로 저장해 다른 월드에 빠르게 적용할 수 있습니다 |
| **안전 보호** | 위험한 NBT 아이템, 커맨드 블록 악용, 폭발 등을 자동으로 차단합니다 |

---

## 명령어

### 일반 명령어

별칭: `/privateworld`, `/프라이빗월드`, `/pv`

| 명령어 | 설명 |
|--------|------|
| `/privateworld create` | 새 개인 월드를 생성합니다 |
| `/privateworld delete` | 현재 있는 본인 월드를 삭제합니다 |
| `/privateworld option` | 현재 월드의 옵션 설정 GUI를 엽니다 |
| `/privateworld visit` | 다른 플레이어 목록 GUI를 열어 월드를 방문합니다 |
| `/privateworld myworld` | 내 월드 목록 GUI를 엽니다 |
| `/privateworld lobby` | 로비로 이동합니다 |
| `/privateworld ban <플레이어>` | 해당 플레이어를 내 월드에서 밴합니다 |
| `/privateworld unban <플레이어>` | 해당 플레이어의 밴을 해제합니다 |
| `/privateworld head <플레이어>` | 플레이어 머리 아이템을 인벤토리에 추가합니다 |
| `/privateworld workshop` | 워크샵 GUI를 엽니다 (콘텐츠 탐색 / 내 월드 등록) |
| `/privateworld help` | 도움말을 표시합니다 |

### 관리자 명령어

별칭: `/PrivateWorldAdmin` | 권한: `prs.admin`

| 명령어 | 설명 |
|--------|------|
| `/prsadmin addmap` | 현재 월드를 관리 목록에 추가합니다 |
| `/prsadmin delmap` | 현재 월드를 관리 목록에서 제거합니다 |
| `/prsadmin delall` | 등록된 모든 플레이어 월드를 삭제합니다 |
| `/prsadmin SetLobby` | 현재 위치를 로비로 설정합니다 |
| `/prsadmin Worlds` | 모든 월드 목록 GUI를 엽니다 |
| `/prsadmin ReloadScore` | 스코어보드를 초기화합니다 |
| `/prsadmin reload` | 설정 파일을 다시 로드합니다 |
| `/prsadmin workshoplist` | 워크샵에 등록된 모든 월드·프리셋 목록을 출력합니다 |
| `/prsadmin workshopdelete <이름>` | 설정 프리셋을 삭제합니다 |
| `/prsadmin workshopunpublish <월드명>` | 워크샵에 등록된 월드의 등록을 강제로 취소합니다 |

---

## 월드 옵션

`/privateworld option` 명령어로 열리는 GUI에서 아래 옵션을 토글할 수 있습니다.

| 옵션 | 기본값 | 설명 |
|------|--------|------|
| 월드 비공개 | OFF | 활성화 시 방문자 입장 차단 |
| 아이템 드롭 | OFF | 방문자의 아이템 드롭 허용 |
| 블록 파괴 | OFF | 방문자의 블록 파괴 허용 |
| 블록 설치 | OFF | 방문자의 블록 설치 허용 |
| 활 쏘기 | OFF | 방문자의 활 사용 허용 |
| 커맨드 | OFF | 방문자의 명령어 사용 허용 |
| 블록 상호작용 | OFF | 방문자의 블록 우클릭 허용 |
| 레드스톤 | OFF | 레드스톤 작동 허용 |
| PvP | OFF | 플레이어 간 공격 허용 |
| 폭죽 | OFF | 폭죽 터뜨리기 허용 |
| 날씨 | OFF | 날씨 변화 허용 |
| 시간 고정 | OFF | 낮 시간 고정 |

게임모드, 월드 이름, 스폰 위치도 같은 GUI에서 설정할 수 있습니다.

---

## 워크샵 (콘텐츠 공유)

워크샵은 플레이어가 자신의 월드에서 제작한 **게임 콘텐츠**를 서버 전체에 공개하는 Steam Workshop 방식의 시스템입니다.

### 지원 카테고리

| 카테고리 | 설명 |
|----------|------|
| 🏃 **파쿠르** | 점프맵·장애물 코스 |
| ⚔️ **PvP** | 결투장·아레나 |
| 🏗️ **자유 건축** | 건축물 전시 월드 |
| 🗺️ **어드벤처** | 스토리·탐험 월드 |
| 🎮 **미니게임** | 각종 미니게임 |
| 📖 **기타** | 분류되지 않는 콘텐츠 |

### 등록 흐름

1. 자신의 월드에서 `/privateworld workshop` 실행
2. 메인 화면에서 **"내 월드 등록"** 클릭
3. 콘텐츠 유형 선택 GUI에서 카테고리 선택
4. 채팅으로 제목 입력 → 워크샵에 즉시 등록

### 탐색 & 참가

워크샵 메인 화면에서 원하는 카테고리 아이콘을 클릭하면 등록된 월드 목록이 표시됩니다.  
**좌클릭** = 해당 월드로 순간이동 | **우클릭** = 내 월드 등록 취소 (소유자·OP 전용)

---

## 설치 방법

1. 의존성 플러그인을 서버의 `plugins/` 폴더에 설치합니다 ([의존성](#의존성) 참고)
2. 빌드된 `PrivateWorld-*.jar`를 `plugins/` 폴더에 넣습니다
3. Void 세계 생성을 위해 `VoidGen` 생성기가 서버에 설치되어 있어야 합니다
4. 서버를 재시작하면 `plugins/Prs/` 디렉터리에 설정 파일이 자동 생성됩니다
5. `/prsadmin SetLobby` 로 로비 위치를 설정합니다

### 빌드

```bash
mvn package
```

빌드 결과물: `target/prs-*.jar`

---

## 설정 파일

### `plugins/Prs/config.yml`

```yaml
max-worlds: 3   # 플레이어 1인당 최대 월드 수
```

### `plugins/Prs/world.yml`

월드 목록, 로비 위치 등 런타임 데이터가 저장됩니다 (자동 관리).

### `plugins/Prs/workshop/published.yml`

워크샵에 등록된 월드 정보가 저장됩니다 (자동 관리).

### `plugins/Prs/workshop/presets.yml`

설정 프리셋 데이터가 저장됩니다 (자동 관리).

---

## 패키지 구조

```
prs/
├── privateworld/     # 플러그인 엔트리포인트 (PrivateWorld.java)
├── command/          # 명령어 처리 (UserCommand, AdminCommand, TabComplete)
├── data/             # 데이터 레이어 (UserWorldManager, WorldConfig, WorkshopManager, …)
├── gui/              # 인벤토리 GUI (WorldOptionMenu, GUI_Workshop, GUI_TypeSelect, …)
├── main/             # 이벤트 핸들러 및 채팅 입력 처리 (EventHandler, Chatting, PerWorldCommand)
├── scoreboard/       # 스코어보드 (WorldScoreboard, Helper)
└── world/            # 월드 생성·삭제·관리 (WorldManager, WorldBanPlayer, WorldFileUtils)
```

---

## 의존성

| 플러그인 | 용도 |
|----------|------|
| [Spigot API 1.21.1](https://www.spigotmc.org/) | 서버 API |
| [RedLib](https://github.com/Redempt/RedLib) | GUI 유틸리티 |
| [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) | 패킷 처리 |
| [NBTAPI (item-nbt-api)](https://www.spigotmc.org/resources/nbt-api.7939/) | NBT 보안 검사 |
| VoidGen | 빈 월드(Void) 생성기 |

---

## 라이선스

이 프로젝트는 개인 서버 플러그인으로, 라이선스가 명시되어 있지 않습니다.  
문의: [Perdume](https://github.com/Perdume)

> 1. Some variables initialized at unknown timing
> 2. Create world speedy
> 3. Config improves
> 4. Improved code readability
