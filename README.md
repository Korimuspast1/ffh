# FFH VPN

Минималистичный VPN-клиент для Android на ядре **Xray-core** (TUN, gVisor).
Чёрно-белый интерфейс, никаких встроенных подписок — всё добавляете сами.

## Возможности

* **Подписки**: добавление по ссылке, импорт из base64, списка ссылок, Clash YAML,
  Xray/sing-box JSON или `sub://…`; обновление одной кнопкой или всех сразу;
  автообновление по интервалу из заголовка `profile-update-interval`.
* **Серверы**: VLESS, VMess, Trojan, Shadowsocks (в т. ч. 2022), Hysteria 2,
  WireGuard, HTTP, SOCKS. SSR и TUIC распознаются и показываются, но ядром
  не поддерживаются — помечаются значком.
* **Пинг**: настоящий TCP-замер задержки (не «случайное число»), параллельно,
  с частичным обновлением строк по мере готовности; можно пинговать после
  обновления подписки.
* **Трафик**: остаток, использованный объём и дата окончания из заголовка
  `subscription-userinfo`, полоса прогресса в карточке подписки.
* **Подключение**: TUN от Xray (`xray.tun.fd`), MTU, IPv6, bypass локальных сетей,
  блокировка QUIC, сниффинг, SOCKS/HTTP-прокси на локальных портах, DNS через
  сервер или на устройстве, разделение по приложениям (allow/disallow).
* **Настройки «по максимуму»**: внешний вид, язык, размер шрифта, темы,
  маршрутизация, DNS, ядро, логи, бэкап/восстановление JSON.
* **Темы**: пресеты + собственный JSON (вставляется в поле добавления подписки
  или в «Настройки → Оформление → Тема»).
* **Языки**: русский и английский (плюс системный).

## Формат темы

```json
{
  "backgroundGradientRotationAngle": 0,
  "backgroundGradientColorIntensity": 0,
  "backgroundColors": ["#000000FF", "#000000FF", "#000000FF"],
  "serverRowBackgroundColor": "#050505FF",
  "selectedServerRowColor": "#151515FF",
  "subsHeaderColor": "#0A0A0AFF",
  "buttonColor": "#FFFFFFFF",
  "buttonTextColor": "#000000FF",
  "powerIconColor": "#000000FF",
  "serverRowTitleTextColor": "#FFFFFFFF",
  "serverRowSubTitleTextColor": "#888888FF",
  "topBarButtonsColor": "#FFFFFFFF",
  "supportIconColor": "#FFFFFFFF",
  "profileWebPageIconColor": "#FFFFFFFF",
  "subHeaderButtonColor": "#FFFFFFFF",
  "settingsControlsTintColor": "#FFFFFFFF",
  "subscriptionInfoBackgroundColor": "#080808FF",
  "subscriptionTrafficBackgroundColor": "#111111FF",
  "subscriptionInfoTextColor": "#FFFFFFFF",
  "disclosureHeaderTextColor": "#FFFFFFFF",
  "disclosureSubHeaderTextColor": "#888888FF",
  "serverRowChevronColor": "#FFFFFFFF",
  "additionalOptionsButtonColor": "#FFFFFFFF",
  "buttonTimerColor": "#FFFFFFFF",
  "elipseColors": ["#161616FF", "#090909FF", "#000000FF"],
  "backgroundImageType": "system",
  "buttonImageType": "light"
}
```

Неизвестные поля игнорируются, отсутствующие берутся из текущей темы —
можно присылать частичные темы.

## Сборка

```bash
./gradlew :app:assembleDebug        # APK
./gradlew :app:assembleRelease      # release (без keystore.properties — debug-подпись)
./gradlew :app:testDebugUnitTest    # тесты парсеров и тем
```

Ядро Xray скачивается скриптом `.github/scripts/fetch-xray.sh` и укладывается в
`app/src/main/jniLibs/<abi>/libxray.so` (arm64-v8a, x86_64) вместе с `geoip.dat`
и `geosite.dat`. Готовые APK собираются в GitHub Actions и публикуются в каталог
`dist/` этой же ветки.

## Требования

Android 8.0 (API 26) и новее.
