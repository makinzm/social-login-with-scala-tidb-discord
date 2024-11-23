#!/bin/bash

# スクリプトがエラーで停止するように設定
set -e

# .env ファイルの存在を確認
ENV_FILE=".env"
if [ ! -f "$ENV_FILE" ]; then
  echo "エラー: $ENV_FILE ファイルが見つかりません。"
  exit 1
fi

# .env ファイルから環境変数をエクスポートする関数
export_env_vars() {
  while IFS= read -r line || [ -n "$line" ]; do
    # 行の前後の空白を削除
    line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

    # 空行またはコメント行をスキップ
    if [[ -z "$line" || "$line" == \#* ]]; then
      continue
    fi

    # インラインコメントを削除（# 以降）
    line=$(echo "$line" | sed 's/#.*//')

    # 'export' キーワードがあれば削除
    if [[ "$line" == export* ]]; then
      line=${line#export }
    fi

    # キーと値を分割
    key=$(echo "$line" | cut -d '=' -f 1)
    value=$(echo "$line" | cut -d '=' -f 2-)

    # 値の前後の空白を削除
    value=$(echo "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

    # 値が引用符で囲まれている場合、引用符を削除
    if [[ "$value" == \"*\" || "$value" == \'*\' ]]; then
      value=${value:1:-1}
    fi

    # 環境変数をエクスポート
    export "$key"="$value"
  done < "$ENV_FILE"
}

# 環境変数をエクスポート
export_env_vars

# sbt run を実行
sbt run

