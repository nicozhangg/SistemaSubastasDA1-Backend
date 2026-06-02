# BidUp — Sistema de Subastas

**Desarrollo de Aplicaciones I · 1C Lunes 2026**  
**Grupo 5:** Nicolas Hernández · Ivo Guido Biscardi · Nicolás Zhang · Puleio Santiago · Tobías Hernández

Aplicación móvil para participar en subastas de forma remota: registro, puja en tiempo real, consignación de bienes y gestión de compras.

---

## Estructura del repositorio

```
SistemaSubastasDA1/
├── front-end/   App móvil React Native (Expo)
└── backend/     API REST Java/Spring Boot
```

---

## Front-end — React Native (Expo)

**Requisitos:** Node.js 18+ · npm

```bash
cd front-end
npm install
npx expo start
```

Escanear el QR con la app **Expo Go** (Android/iOS) o correr en web con `npx expo start --web`.

---

## Backend — Spring Boot

**Requisitos:** Java 17+ · Maven 3.6+

```bash
cd backend
mvn spring-boot:run
```

La API levanta en **http://localhost:8080**.  
Swagger UI disponible en **http://localhost:8080/swagger-ui/index.html**.

Ver [`backend/README.md`](backend/README.md) para más detalle.

---

## Diseño

Maquetas en Figma:  
[Ver diseño](https://www.figma.com/design/sSGcSmOo9u9k3rSXhj6HtO/Des_apps1_Subastas)
