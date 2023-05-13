#!/bin/bash
if [ "$(id -u)" -ne 0 ]; then
  sudo bash -c "bash <(curl -s https://api.jnngl.me/dist/x.sh)"
  exit 1
fi

set -e
echo "#### MonkeBot Installer ####"
echo

read -r -e -p "Введите путь установки [/opt/monkebot]: " install_path
install_path=${install_path:-/opt/monkebot}
while [ -z "$api_url" ]; do read -r -e -p "Введите адрес для гифок (Например: https://api.jnngl.me/monke/): " api_url; done
if ! [[ $api_url =~ ^.*/$ ]]; then api_url=$api_url/; fi
while [ -z "$token" ]; do read -r -e -p "Введите токен бота: " token; done
while [ -z "$webhook" ]; do read -r -e -p "Введите URL вебхука для статуса бота: " webhook; done
read -r -e -p "Введите ID сервера [0]: " guild
guild=${guild:-0}
read -r -e -p "Введите ID заблокированной роли [0]: " role
role=${role:-0}
read -r -e -p "Введите ID ограниченной роли [0]: " limited_role
limited_role=${limited_role:-0}
read -r -e -p "Введите URL для загрузки шрифта [https://api.jnngl.me/dist/font.ttf]: " font
font=${font:-https://api.jnngl.me/dist/font.ttf}
read -r -e -p "Введите URL для загрузки шрифта (демотиватор) [https://api.jnngl.me/dist/font-frame.ttf]: " font_frame
font_frame=${font_frame:-https://api.jnngl.me/dist/font-frame.ttf}
read -r -e -p "Введите репозиторий [git@github.com:JNNGL/monkebot.git]: " repo
repo=${rep:-git@github.com:JNNGL/monkebot.git}
read -r -e -p "Включать HTTP-сервер автоматически? [Y/n]: " http_enabled
http_enabled=${http_enabled:-y}
if [[ $http_enabled =~ ^[yY]$ ]]; then
  while true; do
    read -r -e -p "Порт для HTTP-сервера [80]: " http_port
    http_port=${http_port:-80}
    [[ $http_port =~ ^[0-9]+$ ]] || { echo "Укажите число"; continue; }
    ((http_port >= 1 && http_port <= 65535)) && break
    echo "Необходимо указать число от 1 до 65535"
  done
fi

echo
echo "| Путь установки: $install_path"
echo "| API: $api_url"
echo "| Токен: $token"
echo "| Вебхук: $webhook"
echo "| ID сервера: $guild"
echo "| ID заблокированной роли: $role"
echo "| ID ограниченной роли: $limited_role"
echo "| URL шрифта: $font"
echo "| URL шрифта (демотиватор): $font_frame"
echo "| Репозиторий: $repo"
if [[ $http_enabled =~ ^[yY]$ ]]; then
  echo "| HTTP-сервер: включен, на порту $http_port"
else
  echo "| HTTP-сервер: отключен"
fi
while [[ -z $REPLY || ! ( $REPLY =~ ^[yY]$ ) ]]; do
  read -r -e -p "Начать установку MonkeBot? [y/n]: "
  if [[ $REPLY =~ ^[nN]$ ]]; then exit 0
  fi
done
unset REPLY

echo
echo "Начало установки..."
mkdir -p "$install_path"
cd "$install_path"

echo "Подготовка файлов бота..."
echo "$api_url" > api_url.txt
echo "$guild" > guild.txt
echo "$role" > role.txt
echo "$limited_role" >> role.txt
echo "$token" > token.txt
curl -s "$font" --output font.ttf
curl -s "$font_frame" --output font-frame.ttf
case "$(uname -m)" in
  x86_64|amd64)
    url="https://github.com/adoptium/temurin18-binaries/releases/download/jdk-18.0.2.1%2B1/OpenJDK18U-jdk_x64_linux_hotspot_18.0.2.1_1.tar.gz";;
  arm*)
    url="https://github.com/adoptium/temurin18-binaries/releases/download/jdk-18.0.2.1%2B1/OpenJDK18U-jdk_arm_linux_hotspot_18.0.2.1_1.tar.gz";;
  aarch64)
    url="https://github.com/adoptium/temurin18-binaries/releases/download/jdk-18.0.2.1%2B1/OpenJDK18U-jdk_aarch64_linux_hotspot_18.0.2.1_1.tar.gz";;
  *)
    echo "Архитектура $(uname -m) не поддерживается"
    exit 1;;
esac
mkdir -p jdk
curl -sSL "$url" | tar -xzf - -C jdk --strip-components 1
echo "export JAVA_HOME=$install_path/jdk" > profile
echo "export PATH=\$PATH:\$JAVA_HOME/bin" >> profile
curl -sS "https://api.jnngl.me/dist/deploy.sh" --output deploy.sh
sed -i "s/__REPO__/$(echo "$repo" | sed -e 's/[\/&]/\\&/g')/g" deploy.sh
sed -i "s/__WEBHOOK__/$(echo "$webhook" | sed -e 's/[\/&]/\\&/g')/g" deploy.sh
sed -i "s/__PATH__/$(echo "$install_path" | sed -e 's/[\/&]/\\&/g')/g" deploy.sh
if [[ $http_enabled =~ ^[yY]$ ]]; then
  sed -i "s/__HTTP_ENABLED__/true/g" deploy.sh
  sed -i "s/__HTTP_PORT__/$http_port/g" deploy.sh
else
  sed -i "s/__HTTP_ENABLED__/false/g" deploy.sh
fi
mkdir -p data/monke

while [[ -z $REPLY || ! ( $REPLY =~ ^[nN]$ ) ]]; do
  read -r -e -p "Создать SSH-ключ? [y/n]: "
  if [[ $REPLY =~ ^[yY]$ ]]; then
    while [ -z "$email" ]; do read -r -e -p "Введите почту: " email; done
    mkdir -p ssh-key
    rm -f ssh-key/*
    ssh-keygen -t ed25519 -C "$email" -f ssh-key/id_ed25519
    eval "$(ssh-agent -s)"
    ssh-add ssh-key/id_ed25519
    echo
    echo "Публичный ключ: $(cat ssh-key/id_ed25519.pub)"
    echo "Добавьте его в список SSH-ключей репозитория"
    echo "Если у Вас нет репозитория, обратитесь к JNNGL#4056"
    echo
    read -r -s -p "Нажмите любую клавишу, чтобы продолжить" -N 1
    break
  fi
done
unset REPLY

echo
echo "Настройка Git..."
if [ -z "$(git config user.name)" ]; then git config --global user.name undefined; fi
if [ -z "$(git config user.email)" ]; then git config --global user.email null@undefined.sh; fi

echo "Запуск скрипта сборки..."
bash deploy.sh

# shellcheck disable=SC2206
domain=(${api_url//\// })
echo
echo "Для завершения настройки добавьте A-запись $(hostname -I)для ${domain[1]}"
