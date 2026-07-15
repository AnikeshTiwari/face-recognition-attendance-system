import cv2
import face_recognition
import os
import numpy as np
from datetime import datetime
from db_config import connect_db

# 🕒 Map current hour to class period
def get_current_period():
    hour = datetime.now().hour
    return {
        9: 1, 10: 2, 11: 3, 12: 4,
        14: 5, 15: 6, 16: 7
    }.get(hour, None)

# 📂 Load known faces from folder
def load_known_faces(folder="known_faces"):
    known_faces = []
    known_ids = []
    for filename in os.listdir(folder):
        if filename.endswith(".jpg"):
            student_id = os.path.splitext(filename)[0]
            image = face_recognition.load_image_file(os.path.join(folder, filename))
            encodings = face_recognition.face_encodings(image)
            if encodings:
                known_faces.append(encodings[0])
                known_ids.append(student_id)
            else:
                print(f"⚠️ No face found in {filename}, skipping.")
    return known_faces, known_ids

# 📸 Capture frame from camera
def capture_frame():
    cap = cv2.VideoCapture(0)
    ret, frame = cap.read()
    cap.release()
    return ret, frame

# 🧠 Recognize faces
def recognize_faces(frame, known_faces, known_ids):
    rgb_frame = cv2.cvtColor(cv2.resize(frame, (0, 0), fx=0.25, fy=0.25), cv2.COLOR_BGR2RGB)
    face_locations = face_recognition.face_locations(rgb_frame)
    face_encodings = face_recognition.face_encodings(rgb_frame, face_locations)

    present_ids = set()
    for encoding in face_encodings:
        matches = face_recognition.compare_faces(known_faces, encoding)
        face_distances = face_recognition.face_distance(known_faces, encoding)
        best_match = np.argmin(face_distances)
        if matches[best_match]:
            present_ids.add(known_ids[best_match])
    return present_ids

# ✅ Main logic
def main():
    known_faces, known_ids = load_known_faces()
    period = get_current_period()
    today = datetime.now().date()

    db = connect_db()
    cursor = db.cursor()

    if period is None:
        print("⏳ Not a class period. Skipping attendance.")
        cursor.execute("INSERT INTO logs (action, user) VALUES (%s, %s)", ("Skipped attendance - outside class hours", "cctv_bot"))
        db.commit()
        cursor.close()
        db.close()
        return

    ret, frame = capture_frame()
    if not ret:
        print("❌ Failed to capture frame.")
        cursor.execute("INSERT INTO logs (action, user) VALUES (%s, %s)", ("Camera error - frame not captured", "cctv_bot"))
        db.commit()
        cursor.close()
        db.close()
        return

    present_ids = recognize_faces(frame, known_faces, known_ids)

    for student_id in known_ids:
        status = 'present' if student_id in present_ids else 'absent'

        # Prevent duplicate entries
        cursor.execute("""
            SELECT COUNT(*) FROM attendance
            WHERE student_id = %s AND date = %s AND period = %s
        """, (student_id, today, period))
        already_marked = cursor.fetchone()[0]

        if already_marked == 0:
            cursor.execute("""
                INSERT INTO attendance (student_id, date, period, status)
                VALUES (%s, %s, %s, %s)
            """, (student_id, today, period, status))
            print(f"{student_id} marked {status} for Period {period}")
        else:
            print(f"{student_id} already marked for Period {period}, skipping.")

    cursor.execute("INSERT INTO logs (action, user) VALUES (%s, %s)", (f"Marked attendance for Period {period}", "cctv_bot"))
    db.commit()
    cursor.close()
    db.close()

if __name__ == "__main__":
    main()
