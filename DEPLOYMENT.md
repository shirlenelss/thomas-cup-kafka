# Database Deployment Guide

## 🏗️ **Database Setup for Different Environments**

### 📍 **Local Development (Docker Compose)**
**No manual setup needed** - Docker handles everything automatically:

```bash
# Docker Compose creates database + user automatically
docker compose up -d postgres

# Flyway handles schema migrations when app starts
mvn spring-boot:run
```

### 🌐 **Production/External PostgreSQL**

When deploying to external PostgreSQL instances (AWS RDS, Azure Database, etc.), you need **manual database setup** before running the application.

#### **Step 1: Run Database Initialization**

```bash
# Set environment variables
export POSTGRES_USER=postgres        # PostgreSQL admin user
export POSTGRES_DB=postgres          # Default database to connect to
export DB_PASSWORD=your_app_password  # Password for thomas_cup_user

# Run the initialization script
./docker/init-db.sh
```

#### **Step 2: Configure Application**

Update `application.properties` or set environment variables:

```properties
# Update database URL for your environment
spring.datasource.url=jdbc:postgresql://your-db-host:5432/thomas_cup_dev
spring.datasource.username=thomas_cup_user
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

# Update Kafka bootstrap servers
spring.kafka.bootstrap-servers=your-kafka-host1:9092,your-kafka-host2:9092
```

#### **Step 3: Deploy Application**

```bash
# Set the database password
export SPRING_DATASOURCE_PASSWORD=your_app_password

# Run the application - Flyway will create tables automatically
java -jar target/thomas-cup-kafka-*.jar
```

## 🔄 **What Each Component Handles**

### **`docker/init-db.sh`** (Database/User Setup)
- ✅ Creates database: `thomas_cup_dev`
- ✅ Creates user: `thomas_cup_user` 
- ✅ Sets up permissions and ownership
- ❌ Does NOT create tables/schema

### **Flyway Migrations** (Schema Management)
- ✅ Creates tables (`match_results`)
- ✅ Handles schema changes and versioning
- ✅ Runs automatically when Spring Boot starts
- ❌ Does NOT create database or users

### **Docker Compose** (Local Only)
- ✅ Creates database + user via environment variables
- ✅ Manages PostgreSQL container lifecycle
- ❌ Only works for local Docker development

## 🚀 **Deployment Scenarios**

### **Scenario 1: AWS RDS PostgreSQL**
```bash
# 1. Create RDS instance via AWS Console/CLI
# 2. Run init script against RDS endpoint
POSTGRES_USER=postgres POSTGRES_DB=postgres DB_PASSWORD=mypass ./docker/init-db.sh

# 3. Deploy Spring Boot app with RDS connection string
export SPRING_DATASOURCE_PASSWORD=mypass
java -jar app.jar
```

### **Scenario 2: Azure Database for PostgreSQL**
```bash
# 1. Create Azure PostgreSQL instance
# 2. Run init script against Azure endpoint  
POSTGRES_USER=azureuser POSTGRES_DB=postgres DB_PASSWORD=mypass ./docker/init-db.sh

# 3. Deploy app with Azure connection details
export SPRING_DATASOURCE_PASSWORD=mypass  
java -jar app.jar
```

### **Scenario 3: Kubernetes with External DB**
```yaml
# ConfigMap for connection details
apiVersion: v1
kind: ConfigMap
metadata:
  name: thomas-cup-config
data:
  spring.datasource.url: "jdbc:postgresql://postgres-service:5432/thomas_cup_dev"
  spring.datasource.username: "thomas_cup_user"

---
# Secret for password
apiVersion: v1
kind: Secret
metadata:
  name: thomas-cup-secrets
data:
  spring.datasource.password: <base64-encoded-password>
```

## ⚠️ **Important Notes**

1. **Always run `init-db.sh` BEFORE starting the Spring Boot application**
2. **Use environment variables for passwords** - never hardcode them
3. **Flyway migrations run automatically** when the app starts
4. **Database connection must be working** before Flyway can create tables

## 🔧 **Troubleshooting**

### **"Database thomas_cup_dev does not exist"**
```bash
# Run the init script first
./docker/init-db.sh
```

### **"Role thomas_cup_user does not exist"**  
```bash
# Check PostgreSQL connection and run init script
psql -h your-host -U postgres -d postgres
# Then run ./docker/init-db.sh
```

### **Flyway migration fails**
```bash
# Ensure database and user exist first
# Check application.properties connection details
# Verify SPRING_DATASOURCE_PASSWORD is set
```