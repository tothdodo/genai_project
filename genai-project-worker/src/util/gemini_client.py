import os
import logging
from google import genai
from threading import Lock

class GeminiClient:
    _instance = None
    _lock = Lock()

    def __new__(cls):
        """
        Thread-safe implementation of the Singleton pattern.
        """
        if not cls._instance:
            with cls._lock:
                if not cls._instance:
                    cls._instance = super(GeminiClient, cls).__new__(cls)
                    cls._instance._initialize()
        return cls._instance

    def _initialize(self):
        """
        Initializes the Gemini API client.
        This is called only once when the first instance is created.
        """
        self.api_key = os.getenv("GEMINI_API_KEY")
        if not self.api_key:
            logging.error("GEMINI_API_KEY environment variable not set.")
            raise ValueError("GEMINI_API_KEY environment variable not set.")

        try:
            self.client = genai.Client(api_key=self.api_key)
            logging.info("Gemini API configured successfully.")
        except Exception as e:
            logging.error(f"Failed to configure Gemini API: {e}")
            raise e

    def list_models(self):
        """
        Lists available models and their supported generation methods.
        Useful for debugging '404 Not Found' errors.
        """
        try:
            logging.info("Fetching list of available models...")
            return self.client.models.list()
        except Exception as e:
            logging.error(f"Error listing models: {e}")
            raise e

    def generate_content(self, prompt: str, model_name: str = "gemini-flash-latest") -> str:
        """
        Generates content using the specified Gemini model.

        Args:
            prompt (str): The input text/prompt for the model.
            model_name (str): The model version to use.

        Returns:
            str: The generated text response.
        """
        try:
            response = self.client.models.generate_content(
                model=model_name,
                contents=prompt
            )

            if not response.candidates:
                logging.warning("Gemini response blocked or empty.")
                return "Error: Content generation blocked by safety filters."

            return response.text
        except Exception as e:
            logging.error(f"Error during Gemini generation: {e}")
            raise e