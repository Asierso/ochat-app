<img src="ochat.png" width="48px">

# OCHAT - Ollama Chat
Ochat is a chat Android app client for Ollama AI. Ochat supports chats with different Ollama models

>[!NOTE] 
>Ochat doesn't provide an integrated ollama API server. You need to deploy Ollama server by your way to use Ochat

## 📱 Requirements
To use this app you must need 
- Android 8.0 or upper
- Ollama API service access

## 🔨 Building
To build the project, clone it using `git clone https://github.com/Asierso/ochat-app` and open it in Android Studio
- Project is prepared to compile with SDK 33 (Android 13)

## 🐳 Deploy Ollama in Docker
To deploy Ollama to use with ochat, is better to use Docker containers. You can check the steps [here](https://hub.docker.com/r/ollama/ollama)

## 💻 Deploy Ollama in Termux
If you want to run ochat-app without having Ollama deployed by yourself, you can do it using Termux in your mobile phone

>[!ALERT] 
>Models performance maybe injuried using Termux

- Download Termux app on your mobile phone [here](https://github.com/termux/termux-app)
- Open the app and run the following command: `pkg update ; pkg install wget -y ; wget https://raw.githubusercontent.com/wahasa/Debian/main/Install/debian12.sh ; chmod +x debian12.sh ; ./debian12.sh`

WORK IN PROGRESS