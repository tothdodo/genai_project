import logging
import os
from util.gemini_client import GeminiClient

logging.basicConfig(level=logging.INFO, format='%(message)s')


def main():
    if not os.getenv("GEMINI_API_KEY"):
        print("Error: GEMINI_API_KEY is not set.")
        return

    client = GeminiClient()

    print(f"{'MODEL NAME':<40} | {'DISPLAY NAME'}")
    print("-" * 70)

    try:
        for model in client.list_models():
            print(f"{model.name:<40} | {model.display_name}")

    except Exception as e:
        print(f"Failed to list models: {e}")


if __name__ == "__main__":
    main()