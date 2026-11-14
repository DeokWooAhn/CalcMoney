# 결정 기록

코드와 커밋에 남지 않는 결정과 그 이유를 적습니다. 대화에서 정하고 끝나면 다음 작업 때 사람도 AI도 모릅니다.
예를 들어 "영어 스토어 이미지의 큰 제목에 엔화를 쓰지 않는다"는 코드 어디에도 없습니다.

## 언제 읽나

- 스토어 이미지·등록 문구·소개 글을 만들거나 고치기 전
- 버그처럼 보이는 동작을 고치기 전. 일부러 정한 동작일 수 있습니다.

아래 목록에서 주제가 맞는 기록을 엽니다. 목록으로 못 찾으면 `grep -ril <낱말> docs/decisions/`.

## 언제 쓰나

- 대화에서 코드에 남지 않는 결정(정책, 문구 방향, 하지 않기로 한 것)을 정했으면 **그 작업을 마치기 전에** 씁니다.
- 이미 있는 결정을 바꿨으면 새 파일을 만들지 않고 그 기록을 고칩니다. 맨 아래 "바뀐 기록"에 날짜와 이유를 남깁니다.
- 코드를 고칠 때마다 알아야 하는 규칙과 함정은 여기가 아니라 `AGENTS.md`와 `.claude/skills/`에 둡니다.
  여기에는 특정 작업 때만 필요한 결정을 둡니다.

## 형식

파일 하나에 결정 하나. 파일 이름은 주제(`store-copy-…`)로 짓고 날짜는 본문에 적습니다.

```markdown
# <규칙을 한 문장으로>

- 정한 날: YYYY-MM-DD
- 근거: PR 번호, 대화, 파일
- 적용: 어디에 해당하는지

## 규칙
## 이유
## 예외
## 바뀐 기록
```

"이유"에는 그때 확인한 사실(기기, OS 버전, 날짜)을 함께 적습니다. 사실이 바뀌면 결정도 다시 봅니다.
예외나 바뀐 기록이 없으면 그 절은 뺍니다. 필요하면 `## 코드`(관련 파일)나 `## 확인 방법`을 더합니다.

## 목록

| 기록 | 한 줄 |
| --- | --- |
| [currency-list-and-names.md](currency-list-and-names.md) | 통화 목록은 서버가 정하고, 순서·국기·이름은 앱이 만든다 |
| [store-screenshots-real-app.md](store-screenshots-real-app.md) | 스토어 이미지는 실제 앱 화면을 찍어 만들고 값을 합성하지 않는다 |
| [store-copy-calculator-not-discount.md](store-copy-calculator-not-discount.md) | 앱을 할인 계산기로 소개하지 않는다 |
| [store-copy-currency-in-headline.md](store-copy-currency-in-headline.md) | 여러 나라가 보는 스토어 제목에 특정 통화를 앞세우지 않는다 |
| [offline-claim-condition.md](offline-claim-condition.md) | "오프라인에서도 된다"는 저장된 환율이 있을 때로 한정해 말한다 |
| [calculator-intent-ordering.md](calculator-intent-ordering.md) | 두 플랫폼 계산기는 인텐트가 겹쳐도 같은 순서로 동작한다(확정·저장·조회 분리) |
