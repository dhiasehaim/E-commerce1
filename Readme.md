to test in postmen 

1 in vscode terminal start all services 
docker-compose down
docker-compose up --build

2 curl.exe -X POST http://localhost:8080/auth/init-demo lazam tjik hak Demo users initialized 

3 in postmen
admin login 
http://localhost:8080/auth/login
Content-Type: application/json
{
    "username": "admin",
    "password": "admin123"
}
save the token 

4 get orders with admin token (without admin token it says not autherized)
http://localhost:8080/api/orders

customers:http://localhost:8080/api/customers
invetory:http://localhost:8080/api/inventory

payments:http://localhost:8080/api/payments/all

shpping:http://localhost:8080/api/shipping/all

notification:http://localhost:8080/api/notifications

