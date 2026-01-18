import logging
import os
from util.gemini_client import GeminiClient

# Configure simple logging for this script
logging.basicConfig(level=logging.INFO, format='%(message)s')


def main():
    # Ensure API Key is set in your environment before running
    if not os.getenv("GEMINI_API_KEY"):
        print("Error: GEMINI_API_KEY is not set.")
        return

    client = GeminiClient()

    print(f"{'MODEL NAME':<40} | {'DISPLAY NAME'}")
    print("-" * 70)

    try:
        # Iterate through the models yielded by the SDK
        for model in client.list_models():
            # In the new SDK, we check if 'generateContent' is in supported_actions (or similar attributes)
            # Depending on the exact SDK version, attributes might vary, so we print broadly first.
            print(f"{model.name:<40} | {model.display_name}")

    except Exception as e:
        print(f"Failed to list models: {e}")


if __name__ == "__main__":
    main()