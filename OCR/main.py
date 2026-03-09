from pathlib import Path
import subprocess


LANG = "rus+eng" 
TESSDATA_DIR = Path(__file__).with_name("tessdata")


def ask_image_path() -> Path | None:
    """
    самый простой функционал
    """
    raw = input("\nУкажи путь к изображению (или Enter / q для выхода): ").strip().strip('"')
    if not raw or raw.lower() in {"q", "quit", "exit"}:
        return None

    path = Path(raw).expanduser()
    if not path.is_file():
        print(f"[!] Файл не найден: {path}")
        return ask_image_path()

    return path


def ocr_image(image_path: Path) -> str:
    """
    ставим утилиту sudo apt install -y tesseract-ocr tesseract-ocr-rus
    """
    cmd = [
        "tesseract",
        str(image_path),
        "stdout",
        "-l",
        LANG,
        "--tessdata-dir",
        str(TESSDATA_DIR),
    ]

    result = subprocess.run(
        cmd,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="ignore",
    )

    if result.returncode != 0:
        raise RuntimeError(f"Tesseract error: {result.stderr.strip()}")

    return result.stdout


def main() -> None:
    print(f"=== OCR консольный бот (через tesseract.exe), язык: {LANG} ===")

    while True:
        img_path = ask_image_path()
        if img_path is None:
            print("Выход.")
            break

        try:
            text = ocr_image(img_path)
            print("\n--- Распознанный текст ---")
            print(text if text.strip() else "[пусто / ничего не распознано]")
            print("--------------------------")
        except Exception as e:
            print(f"[!] Ошибка при распознавании: {e}")


if __name__ == "__main__":
    main()