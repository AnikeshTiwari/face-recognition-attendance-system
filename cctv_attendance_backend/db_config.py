import mysql.connector

def connect_db():
    return mysql.connector.connect(
        host="localhost",
        user="root",
        password="",  # Leave blank unless you set a password in XAMPP
        database="attendance_system"
    )