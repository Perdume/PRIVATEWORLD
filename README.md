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
- [워크샵 스크립트 (코딩)](#워크샵-스크립트-코딩)
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
| **스크립트 코딩** | 책과 깃펜에 직접 코드를 써서 체크포인트·타이머·미니게임 규칙을 만들고 워크샵에 공개할 수 있습니다 |
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
| `/privateworld script` | 내 스크립트 GUI를 엽니다 (코딩 / 설치 / 공개) |
| `/privateworld script guide` | 스크립트 문법 가이드 책을 엽니다 |
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
| `/prsadmin scriptlist` | 모든 스크립트 목록을 출력합니다 |
| `/prsadmin scriptunpublish <이름\|ID>` | 스크립트의 공개를 강제로 취소합니다 |
| `/prsadmin scriptdelete <이름\|ID>` | 스크립트를 삭제합니다 |

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

## 워크샵 스크립트 (코딩)

플레이어가 **책과 깃펜에 직접 코드를 써서** 자기 월드의 게임 규칙(체크포인트, 기록 타이머, 미니게임 점수 등)을 만들고,
완성되면 워크샵에 **공개**해 다른 플레이어도 자기 월드에 설치할 수 있게 하는 기능입니다.

### 흐름

1. `/privateworld script` 또는 워크샵 메인의 **"내 스크립트 코딩"** → **"새 스크립트 만들기"**
2. 채팅으로 이름 입력 → 예제 코드가 들어 있는 **책과 깃펜**을 받습니다
3. 책에 코드를 쓰고 **'완료'** 를 누르면 저장되며, 오류가 있으면 **줄 번호와 함께** 알려줍니다
4. 스크립트 상세 화면에서 **"이 월드에 설치"** → 본인 월드에서는 저장 즉시 반영되어 바로 테스트할 수 있습니다
5. **"워크샵에 공개"** → 다른 플레이어가 **"스크립트 워크샵"** 에서 찾아 자기 월드에 설치할 수 있습니다
   (다른 사람 월드에는 마지막으로 공개한 버전이 실행되며, 수정 후 **"공개 버전 업데이트"** 로 갱신합니다)

### 문법

```text
# 주석
on <이벤트>          # 이벤트 블록 시작 (다음 on 까지)
  <명령어>           # 한 줄에 하나, 들여쓰기는 자유
  if coins >= 10     # 조건 (else / else if 가능, end 로 닫기)
    message "부자!"
  end
```

| 이벤트 | 설명 |
|--------|------|
| `on join` / `on leave` | 월드 입장 / 퇴장 |
| `on step <블록>` / `on step at x y z` | 블록을 밟을 때 (발판·카펫 포함) |
| `on click <블록>` / `on click at x y z` | 블록 우클릭 (버튼·레버 등) |
| `on fall <y>` | Y 좌표 아래로 떨어질 때 |
| `on death` / `on kill` | 사망 (부활 후 실행) / 다른 플레이어 처치 |
| `on chat "<내용>"` | 특정 채팅을 입력할 때 |
| `on timer <초>` | N초마다, 월드 안 플레이어마다 실행 |

| 명령어 | 설명 |
|--------|------|
| `message` / `broadcast` / `title` / `actionbar` `"텍스트"` | 메시지 (`&` 색 코드, `{player}` `{time}` `{변수}` 치환) |
| `sound <키>` / `particle <이름> [개수]` | 효과음 (`entity.player.levelup`) / 파티클 |
| `teleport x y z` · `teleport ~ ~5 ~` · `teleport spawn\|checkpoint` | 같은 월드 안에서만 이동 |
| `checkpoint` / `respawn` / `lobby` | 체크포인트 저장 / 체크포인트로 이동 / 로비로 |
| `give <아이템> [개수]` / `clear` / `heal` / `damage <양>` | 아이템·체력 (커맨드 블록 등 위험 아이템은 금지) |
| `gamemode survival\|adventure\|spectator` | 게임모드 (크리에이티브 불가) |
| `effect <효과> [초] [레벨]` / `effect clear` | 포션 효과 (최대 600초, 10레벨) |
| `set <변수> <값>` / `add <변수> <값>` | 변수 (`$`로 시작하면 월드 공용) |
| `timer start` / `timer stop` | 스톱워치 (`time` / `{time}`) |
| `wait <초>` / `stop` | 대기 (최대 60초) / 남은 줄 실행 중단 |

내장 값: `time`, `x`, `y`, `z`, `health`, `food`, `players`, `random`(0~100)

### 예제: 파쿠르

```text
on join
  message "&a{player}님 환영합니다!"
  checkpoint
  timer start

on fall 0
  respawn

on step gold_block
  checkpoint
  actionbar "&e체크포인트 저장!"

on step diamond_block
  timer stop
  broadcast "&b{player}님 {time}초 클리어!"
  sound ui.toast.challenge_complete
```

### 안전장치

- 스크립트는 **설치된 월드 안에서, 이벤트를 일으킨 플레이어에게만** 작동합니다 (서버 명령어 실행 불가)
- 월드를 나가면 스크립트가 건 포션 효과와 바꾼 게임모드는 원래대로 돌아갑니다
- 한 번 실행에 최대 명령 수, 월드당 대기 중인 실행 수, 코드 길이 등이 `config.yml` 로 제한됩니다
- 실행 중 오류는 월드 주인에게 줄 번호와 함께 알려줍니다
- 스크립트가 설치된 월드에서 죽으면 체크포인트(없으면 월드 스폰)에서 부활합니다

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

script:
  enabled: true                 # 스크립트 기능 사용 여부
  max-scripts-per-player: 10    # 1인당 만들 수 있는 스크립트 수
  max-installed-per-world: 5    # 한 월드에 설치할 수 있는 스크립트 수
  max-source-length: 8000       # 코드 최대 길이(글자)
  max-actions-per-run: 200      # 이벤트 1회당 실행할 수 있는 최대 명령 수
  max-pending-per-world: 100    # 월드당 wait 로 대기 중인 실행 수
```

### `plugins/Prs/world.yml`

월드 목록, 로비 위치 등 런타임 데이터가 저장됩니다 (자동 관리).

### `plugins/Prs/workshop/published.yml`

워크샵에 등록된 월드 정보가 저장됩니다 (자동 관리).

### `plugins/Prs/workshop/presets.yml`

설정 프리셋 데이터가 저장됩니다 (자동 관리).

### `plugins/Prs/workshop/scripts.yml`

플레이어가 작성한 스크립트(작성 중 코드 / 공개된 코드 / 버전 / 설치 수)가 저장됩니다 (자동 관리).
월드에 설치된 스크립트 목록은 `plugins/PRSUSERSETTING/<월드>.yml` 의 `Scripts` 에 저장됩니다.

---

## 패키지 구조

```
prs/
├── privateworld/     # 플러그인 엔트리포인트 (PrivateWorld.java)
├── command/          # 명령어 처리 (UserCommand, AdminCommand, TabComplete)
├── data/             # 데이터 레이어 (UserWorldManager, WorldConfig, WorkshopManager, ScriptManager, …)
├── gui/              # 인벤토리 GUI (WorldOptionMenu, GUI_Workshop, GUI_Workshop_Scripts, …)
├── main/             # 이벤트 핸들러 및 채팅 입력 처리 (EventHandler, Chatting, PerWorldCommand)
├── scoreboard/       # 스코어보드 (WorldScoreboard, Helper)
├── script/           # 워크샵 스크립트 언어 (ScriptParser, ScriptRunner, ScriptListener, ScriptBooks)
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
