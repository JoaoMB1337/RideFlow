# RideFlow

App para treinar indoor com a **bicicleta de interior Van Rysel D100** (Decathlon).

A D100 fala Bluetooth **FTMS** (Fitness Machine Service). O RideFlow liga-se a ela
diretamente, sem apps de terceiros, e a um cinto de frequência cardíaca BLE.

## O que faz

- Lê potência, velocidade e distância da D100 em tempo real (Indoor Bike Data, `0x2AD2`).
- Controla a resistência da bicicleta pelo FTMS Control Point.
- Lê frequência cardíaca de qualquer sensor BLE standard (Heart Rate Service).
- Grava cada treino localmente (SQLDelight) com histórico, gráficos e médias/máximos.
- Partilha o resumo de um treino como imagem.

## Ligar a D100

1. Liga a bicicleta e começa a pedalar um pouco para a acordar.
2. Na app: **Settings → Smart bike (FTMS) → Pair** e escolhe a D100 da lista.
3. Opcional: emparelha também o sensor de frequência cardíaca.
4. Os sensores emparelhados voltam a ligar-se automaticamente ao abrir a app.

> A D100 só aceita uma ligação de cada vez. Fecha outras apps (Zwift, Decathlon Coach, …)
> antes de emparelhar.

## Projeto

Kotlin Multiplatform + Compose Multiplatform.

| Pasta        | Conteúdo                                                    |
|--------------|-------------------------------------------------------------|
| `shared/`    | UI (Compose), lógica, BLE (Kable), base de dados (SQLDelight) |
| `androidApp/`| App Android (minSdk 28)                                     |
| `iosApp/`    | App iOS (Xcode)                                             |

## Correr

Android:

```bash
./gradlew :androidApp:installDebug
```

iOS: abrir `iosApp/iosApp.xcodeproj` no Xcode e correr.

Testes:

```bash
./gradlew :shared:testAndroidHostTest
```
