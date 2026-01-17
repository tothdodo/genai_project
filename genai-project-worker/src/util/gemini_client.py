import os
import logging
import google.generativeai as genai
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
            genai.configure(api_key=self.api_key)
            logging.info("Gemini API configured successfully.")
        except Exception as e:
            logging.error(f"Failed to configure Gemini API: {e}")
            raise e

    def generate_content(self, prompt: str, model_name: str = "gemini-pro") -> str:
        """
        Generates content using the specified Gemini model.

        Args:
            prompt (str): The input text/prompt for the model.
            model_name (str): The model version to use (default: 'gemini-pro').

        Returns:
            str: The generated text response.
        """
        try:
            model = genai.GenerativeModel(model_name)
            response = model.generate_content(prompt)

            # Check if response was blocked due to safety settings
            if not response.parts and response.prompt_feedback:
                logging.warning(f"Gemini response blocked. Feedback: {response.prompt_feedback}")
                return "Error: Content generation blocked by safety filters."

            return response.text
        except Exception as e:
            logging.error(f"Error during Gemini generation: {e}")
            # Re-raise or return None depending on how you want to handle failures in workers
            raise e