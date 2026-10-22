# AgroGPS 🚜📱

Primeira base Android do AgroGPS, feita para começar usando somente o GPS integrado do celular.

## O que existe nesta versão inicial
- Tela horizontal de orientação de campo.
- Leitura da posição GPS e precisão informada pelo Android.
- Ações para marcar início e fim de uma linha guia (interface inicial).
- Workflow do GitHub Actions para compilar e publicar o APK de depuração como artefato.

## Limitações atuais
- A visão do campo ainda é ilustrativa; não é um mapa georreferenciado real.
- Marcar início/fim ainda não calcula passadas paralelas reais.
- Não há gravação de talhões nem cálculo de hectares nesta primeira base.
- GPS de celular comum não oferece precisão centimétrica garantida.

## Gerar APK
No GitHub, abra **Actions → Build AgroGPS APK → Run workflow**. Ao terminar, abra a execução e baixe o artefato **AgroGPS-debug-apk**.

Package: `br.com.gcros.agrogps`
