# Cold Mail Automation Tool

A Spring Boot application for automating cold email outreach for job applications.

## Features

- 📧 **Email Templates** - Create reusable email templates with placeholders
- 👥 **Recipient Management** - Manage contacts with status tracking
- 📤 **Bulk Sending** - Send personalized emails to multiple recipients
- 📊 **Dashboard** - Track email statistics and activity
- 📝 **Email Logs** - View history of sent emails

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2
- **Database:** H2 (Development) / PostgreSQL (Production)
- **Frontend:** Thymeleaf, Bootstrap 5
- **Email:** Spring Mail with Gmail SMTP

## Prerequisites

- Java 17+
- Maven 3.6+
- Gmail account with App Password

## Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/cold-mail-automation.git
   cd cold-mail-automation

Set environment variables:

Bash

export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
Run the application:

Bash

mvn spring-boot:run -Dmaven.test.skip=true
Open browser: http://localhost:8080

Template Placeholders
Use these placeholders in your email templates:

{{NAME}} - Recipient's name
{{COMPANY}} - Company name
{{POSITION}} - Job position
{{EMAIL}} - Recipient's email
API Endpoints
Method	Endpoint	Description
GET	/api/templates	Get all templates
POST	/api/templates	Create template
GET	/api/recipients	Get all recipients
POST	/api/recipients	Add recipient
POST	/api/emails/send	Send bulk emails
GET	/api/emails/logs	Get email logs
Screenshots
Dashboard
Dashboard

Send Emails
Send

Future Enhancements
CSV import for bulk recipients
Email scheduling
Resume/CV attachments
Email open tracking
PostgreSQL support
License
MIT License

Author
Your Name - GitHub