# Git Guideline
변수 정의는 `/README.md`를 참고합니다.

## 변수 정의
- `/README.md` 기준 플레이스홀더 사용

## 브랜치 전략
- master : 메인 배포용

## 커밋 메시지 규칙
- [FEAT], [FIX], [DOC], [STYLE], [REFACTOR], [TEST], [CHORE]
- 예시: [FEAT] 데이터 품질 검증 서비스 구현

## 커밋/푸시 명령어 (bash)

### 1) 상태 확인
```bash
git status
git branch
```

### 2) 커밋 (예: 전체 파일)
```bash
git add .
git commit -m "[CHORE] init project skeleton"
```

### 3) 원격(origin) 연결 (처음 1회)
```bash
git remote add origin https://github.com/hdh2680/basemarkdown.git
git remote -v
```

### 4) 푸시
```bash
# 최초 푸시(업스트림 설정)
git push -u origin main

# 이후 푸시
git push
```

## PR 규칙
- 리뷰어 최소 1인 지정
- 모든 테스트 통과 후 병합
- 커밋 메시지와 PR 제목 일치

## 태그/릴리즈
- 릴리즈 태그: v{MAJOR}.{MINOR}.{PATCH}
- GitHub Release Notes 작성