# FFH VPN

Минималистичный черно-белый VPN-клиент на базе **Xray-core** (v26, TUN inbound).
Весь код написан с нуля: парсеры подписок, движок тем, туннель и интерфейс.

## Возможности

* **Подписки**: добавление по URL / base64 / списку ссылок / Clash YAML / JSON,
  обновление вручную и по расписанию, трафик, дата окончания, описание.
* **Сервера**: vless, vmess, trojan, shadowsocks, ssr, hysteria2, tuic, wireguard.
* **Пинг**: реальное измерение задержки (TCP + TLS handshake) и URL-тест через ядро.
* **Туннель**: VpnService на базе TUN inbound Xray-core, DNS, маршрутизация, per-app.
* **Темы**: JSON-темы (вставляются прямо в поле добавления подписки или в Настройках),
  пресеты, импорт/экспорт.
* **Языки**: русский, английский.
* **Настройки**: DNS, MTU, маршрутизация, приложения, логи, бэкап и многое другое.

## Сборка

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK
./gradlew test               # unit-тесты
```

Ядро Xray не лежит в репозитории — его скачивает CI-скрипт:

```bash
bash .github/scripts/fetch-xray.sh v26.3.27
```

Скрипт берет официальные сборки `Xray-android-arm64-v8a` и `Xray-android-amd64`,
раскладывает бинарник как `app/src/main/jniLibs/<abi>/libxray.so` (Android
разрешает исполнять нативные библиотеки, но не файлы из данных приложения) и
кладёт `geoip.dat` / `geosite.dat` в assets.
