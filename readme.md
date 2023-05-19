# Itemfolio 1.1
# 목표
마크 최신버전(1.19.4) 아이템을 해금해보기
## 버전
- paper : 1.19.4-538
- java : 17
- kotlin : 1.8.10

## 특징
- 도감으로 해금할 아이템을 선정합니다.
- 아이템을 해금할때마다 도감에 추가됩니다.
- 도감을 일정 갯수 채울때마다 경품권을 얻습니다.
- 경품권에는 다양한 경품이 있습니다 ^_^
- 도감과 경품권은 yaml 파일로 저장이되어 서버를 껐다 켜도 데이터가 유지됩니다.
- 도감을 검색하여 현재 어떤 아이템이 해금되었는지 확인합니다.

## 명령어 모음
- /help : 명령어 모음
- /해금 <숫자>, /unlock <숫자>, /ul <숫자> : 아이템을 해금합니다.
- /타겟 <숫자>, /target <숫자>, /t <숫자> : 타겟 아이템을 확인합니다.
- /진행도,/진행,/진행률,/progress,/p : 도감 진행도를 확인합니다.
- /도감 <숫자>, /dogam <숫자>, /d <숫자> : 도감을 검색합니다.
- /순위,/rank : 도감 순위를 확인합니다.
- /경품권,/gift,/g : 경품권을 확인합니다.
- /경품권사용,/usegift,/ug : 경품권을 사용합니다.\

## 타겟 파일 설정
- 1.1 버전 이후로 추가된 내용입니다.
- 해금할 아이템을 설정할 수 있습니다.
- 타겟 파일은 `plugins/Itemfolio/target.yml`에 설정해주세요.
- 타겟 파일은 아래와 같은 형식으로 작성해주세요. (타겟 이름: 타겟 해금 갯수)
```yaml
CAULDRON: 10
SPRUCE_DOOR: 10
SHEARS: 10
LEATHER: 10
SPIDER_EYE: 10
HAY_BLOCK: 10
BLACK_GLAZED_TERRACOTTA: 10
```
- 타겟 이름은 [여기](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html)에서 확인할 수 있습니다.
- 샘플 타겟 파일은 `./sampleTarget/target.yml`에 있습니다.