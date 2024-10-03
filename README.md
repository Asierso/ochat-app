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
If you want to run ochat-app without having Ollama deployed by yourself, you can do it using Termux in your mobile phone downloading any linux distro

>[!WARNING] 
>Models performance maybe injuried using Termux

- Download Termux app on your mobile phone [here](https://github.com/termux/termux-app)
- Open the app and run the following command: `pkg update ; pkg install wget -y ; wget https://raw.githubusercontent.com/wahasa/Debian/main/Install/debian12.sh ; chmod +x debian12.sh ; ./debian12.sh`. You will need to grand storage permissions to Termux
- Run in your Termux bash `debian`
- Then, execute `apt update ; apt install curl ca-certificates -y && curl -fsSL https://ollama.com/install.sh | sh` and wait few minutes

Now Ollama should be installed and functional, starting service with `ollama serve` inside Debian

>[!TIP] 
>Optionally you can run `echo "ollama serve &" >> .bashrc` to set up ollama only with entering Termux and execute `debian` 

You can download models using `ollama pull <model-name>`. To access it from the app, set IP to 127.0.0.1 and port to 11434