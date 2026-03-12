# AstroProject

AstroProject is a Java-based application for managing and analyzing astronomical collision data. It features a database management system, CSV export functionality, and a user interface for adding and viewing collision records.

## Features
- Database management for astronomical collisions
- Export collision data to CSV
- Add new collision records via a form
- Modular code structure for easy maintenance

## Project Structure
```
AstroProject/
├── astro_dbms_features.sql      # SQL features and schema
├── collisions_export.csv        # Exported collision data
├── lib/                        # External libraries
├── src/
│   └── astro/
│       ├── AddCollisionForm.java    # Form for adding collisions
│       ├── CollisionApp.java        # Main application
│       ├── CollisionDAO.java        # Data access object
│       ├── DBUtil.java              # Database utilities
│       └── AstroProject.code-workspace
```

## Getting Started
1. Clone the repository:
   ```
   git clone <repo-url>
   ```
2. Open the project in your IDE (e.g., VS Code, IntelliJ).
3. Ensure Java and any required libraries are installed.
4. Build and run the application from `CollisionApp.java`.

## Requirements
- Java 8 or higher
- JDBC-compatible database (e.g., SQLite, MySQL)

## Usage
- Run the application to manage collision records.
- Use the form to add new collisions.
- Export data as CSV for further analysis.

## License
This project is licensed under the MIT License.

## Author
- Rohan Sai Aneeswar 
