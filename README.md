# blls

Языковой сервер для BL

## Сборка языкового сервера

```sh
./gradlew :cli:build
```

## Сборка расширения для VS Code

```sh
cd vscode/
npm install
npx vsce package
```

На выходе: _vscode/blls-*.vsix_

Установить через _Install from VSIX..._

## Сборка расширения для IntelliJ IDEA

```sh
./gradlew :idea:buildPlugin
```

На выходе: _idea/build/distributions/idea-*.zip_

Установить через _Install Plugin from Disk..._
