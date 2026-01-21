# 🚀 Getting Started - WS25 Generative AI Project

Follow these steps to set up the project environment on your local machine.

### 🛠 Prerequisites
* **Docker & Docker Compose:** Ensure you have [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.
* **API Access:** You will need a valid `GEMINI_API_KEY`.

---

### 📦 Installation

1.  **Configure Environment Variables**

    Open the `.env` file and paste your key:
    `GEMINI_API_KEY=paste_api_key_here`

2.  **Spin Up Containers**
    Run the following command in the root directory. The `--build` flag ensures that any local code changes are incorporated:
    ```bash
    docker compose up --build
    ```

3.  **Access the Application**
    Once all containers are running and active, open your browser and navigate to:
    👉 **[http://localhost:3000](http://localhost:3000)**

---

### 🔍 Troubleshooting

| Issue | Solution |
| :--- | :--- |
| **Port already in use** | Stop any other local servers running on a port of a container, or change the port mapping in `docker-compose.yml`. |
| **Docker Engine not found** | Ensure Docker Desktop is open and the engine has finished starting up. |
| **M1/M2/M3 Mac performance** | If builds are slow, ensure "Use Rosetta for x86_64/amd64 emulation" is enabled in Docker settings. |
| **Permission Denied (Linux)** | You may need to run commands with `sudo` or [add your user to the docker group](https://docs.docker.com/engine/install/linux-postinstall/). |
| **Changes not reflecting** | Run `docker compose down` followed by `docker compose up --build` to clear the cache. |