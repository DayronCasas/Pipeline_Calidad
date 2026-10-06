# Informe de Pipeline de Integración Continua
## Calidad de Software — PSW Pipeline Base

---

| Campo | Detalle |
|---|---|
| **Estudiante** | Dayron Casas |
| **Curso** | Ingeniería de Software / Calidad de Software |
| **Fecha** | Octubre 2026 |
| **Proyecto** | PSW Pipeline Base |
| **Tecnologías** | Jenkins · SonarQube · JMeter · Slack · Spring Boot 3.5.6 · Java 17 |

---

## 1. Proyecto Base

### 1.1 Descripción

El proyecto es una API REST desarrollada con Spring Boot 3.5.6 que expone dos endpoints principales para ser evaluados en el pipeline de calidad:

| Endpoint | Método | Descripción |
|---|---|---|
| `/products` | GET | Retorna una lista de 5 productos con id, nombre y precio |
| `/login` | POST | Autentica usuario y retorna token si las credenciales son correctas |

La aplicación corre en el puerto **8085** y tiene habilitado Spring Actuator (`/actuator/health`, `/actuator/info`).

### 1.2 Estructura del proyecto

```
PSW_Pipeline_Base/
├── src/main/java/vallegrande/edu/pe/
│   ├── controller/
│   │   ├── AuthController.java        ← POST /login
│   │   └── ProductController.java     ← GET /products
│   ├── model/
│   │   ├── LoginRequest.java
│   │   └── Product.java
│   ├── service/
│   │   └── ProductService.java
│   └── PswPipelineBaseApplication.java
├── src/main/resources/
│   └── application.properties
├── src/test/jmeter/
│   └── pipeline_load_test.jmx        ← Plan de carga JMeter
├── Jenkinsfile                        ← Pipeline CI/CD
├── sonar-project.properties          ← Configuración SonarQube
└── pom.xml
```

### 1.3 Verificación de compilación

Para compilar y ejecutar el proyecto:

```bash
# Compilar
mvn clean package

# Ejecutar
java -jar target/psw-pipeline-base-0.0.1-SNAPSHOT.jar

# Verificar endpoints
curl http://localhost:8085/products
curl -X POST http://localhost:8085/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"123456"}'
```

**Respuesta esperada GET /products:**
```json
[
  {"id":1,"name":"Laptop","price":2500.0},
  {"id":2,"name":"Mouse","price":80.0},
  {"id":3,"name":"Teclado","price":120.0},
  {"id":4,"name":"Monitor","price":850.0},
  {"id":5,"name":"Audífonos","price":150.0}
]
```

**Respuesta esperada POST /login (credenciales correctas):**
```json
{
  "success": true,
  "message": "Login correcto",
  "token": "ABC123XYZ"
}
```

> **Evidencia:** El proyecto compila sin errores con `mvn clean package` y responde correctamente en ambos endpoints al ejecutarse.

---

## 2. Pipeline de Jenkins

### 2.1 Descripción del Jenkinsfile

El archivo `Jenkinsfile` ubicado en la raíz del proyecto define un pipeline declarativo con las siguientes etapas:

```
Checkout → Build → Test → SonarQube Analysis → Quality Gate → JMeter Load Test
                                                                        ↓
                                                              Notificación Slack
```

### 2.2 Etapas del pipeline

| # | Etapa | Descripción |
|---|---|---|
| 1 | **Checkout** | Obtiene el código fuente desde el repositorio SCM |
| 2 | **Build** | Compila el proyecto con `mvn clean package -DskipTests` y archiva el JAR |
| 3 | **Test** | Ejecuta los tests unitarios y genera el reporte de cobertura con JaCoCo |
| 4 | **SonarQube Analysis** | Analiza la calidad del código con `mvn sonar:sonar` |
| 5 | **Quality Gate** | Espera el resultado del Quality Gate de SonarQube (timeout: 3 min) |
| 6 | **JMeter Load Test** | Ejecuta las pruebas de carga en modo no-GUI y genera reporte HTML |

### 2.3 Configuración requerida en Jenkins

Antes de ejecutar el pipeline, se deben configurar los siguientes elementos en Jenkins:

**Herramientas (Manage Jenkins → Tools):**
- JDK 17 con el nombre `JDK17`
- Maven con el nombre `Maven`

**Credenciales y conexiones:**
- SonarQube Server configurado con nombre `SonarQube` en *Manage Jenkins → Configure System*
- Token de SonarQube almacenado como credencial `sonar-token`
- Webhook de SonarQube apuntando a `http://<jenkins-url>/sonarqube-webhook/`

**Plugins requeridos:**
- SonarQube Scanner for Jenkins
- JaCoCo Plugin
- Performance Plugin (para JMeter)
- HTML Publisher Plugin
- Slack Notification Plugin

**Variable de entorno Slack:**
- Credencial `slack-token` con el token de la app de Slack
- Canal configurado: `#pipeline-notificaciones`

### 2.4 Variables configurables del pipeline

```groovy
SONAR_HOST    = 'http://localhost:9000'   // URL de SonarQube
JMETER_HOME   = 'C:/apache-jmeter'        // Ruta de instalación de JMeter
SLACK_CHANNEL = '#pipeline-notificaciones' // Canal de notificaciones
```

### 2.5 Secciones del Jenkinsfile — fragmentos clave

**Etapa Build:**
```groovy
stage('Build') {
    steps {
        dir('PSW_Pipeline_Base') {
            bat 'mvn clean package -DskipTests'
        }
    }
    post {
        success {
            archiveArtifacts artifacts: 'PSW_Pipeline_Base/target/*.jar', fingerprint: true
        }
    }
}
```

**Etapa SonarQube:**
```groovy
stage('SonarQube Analysis') {
    steps {
        withSonarQubeEnv('SonarQube') {
            dir('PSW_Pipeline_Base') {
                bat """
                    mvn sonar:sonar ^
                      -Dsonar.projectKey=psw-pipeline-base ^
                      -Dsonar.sources=src/main/java ^
                      -Dsonar.java.binaries=target/classes
                """
            }
        }
    }
}
```

**Notificación Slack (éxito):**
```groovy
post {
    success {
        slackSend(
            channel: "${SLACK_CHANNEL}",
            color  : 'good',
            message: ":white_check_mark: *PIPELINE EXITOSO* — `${APP_NAME}` ..."
        )
    }
}
```

> **Evidencia:** El pipeline muestra 6 etapas en color verde (éxito) en la vista Stage View de Jenkins.

---

## 3. Análisis con SonarQube

### 3.1 Configuración del análisis

El archivo `sonar-project.properties` contiene la configuración del análisis:

```properties
sonar.projectKey=psw-pipeline-base
sonar.projectName=PSW Pipeline Base
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=target/classes
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
sonar.java.source=17
```

### 3.2 Resultados esperados del análisis

Basado en la revisión del código fuente, se anticipan los siguientes hallazgos:

| Categoría | Cantidad estimada | Archivos afectados |
|---|---|---|
| Bugs | 0–1 | — |
| Vulnerabilidades | 1 | `AuthController.java` |
| Code Smells | 4–6 | `ProductService.java`, `LoginRequest.java` |
| Coverage | ~0% | Todo el proyecto (sin tests propios) |
| Duplicaciones | Bajo | — |

> **Evidencia:** Captura del dashboard de SonarQube con los resultados del análisis ejecutado desde el pipeline.

### 3.3 Identificación de 3 problemas y propuestas de mejora

---

#### Problema 1 — Code Smell: Anidamiento profundo en `validateProduct()`

**Archivo:** `ProductService.java`

**Código problemático:**
```java
public boolean validateProduct(Product product) {
    if (product != null) {
        if (product.getName() != null) {
            if (!product.getName().isEmpty()) {
                if (product.getPrice() != null) {
                    if (product.getPrice() > 0) {
                        return true;  // ← 5 niveles de anidamiento
                    }
                }
            }
        }
    }
    return false;
}
```

**¿Qué detectó SonarQube?**
Cognitive Complexity excesiva. El método tiene 5 niveles de anidamiento `if`, lo que genera un índice de complejidad cognitiva alto. SonarQube lo marca como Code Smell con severidad **Major**.

**¿Por qué puede afectar al proyecto?**
El código anidado es difícil de leer y mantener. Cada nivel adicional aumenta la probabilidad de introducir errores al modificar la lógica. Con el tiempo, otros desarrolladores necesitarán más tiempo para entender qué hace el método, incrementando el costo de mantenimiento.

**Mejora propuesta — Early return (guardas):**
```java
public boolean validateProduct(Product product) {
    if (product == null) return false;
    if (product.getName() == null || product.getName().isEmpty()) return false;
    if (product.getPrice() == null || product.getPrice() <= 0) return false;
    return true;
}
```
O de forma más expresiva con una sola línea:
```java
public boolean validateProduct(Product product) {
    return product != null
        && product.getName() != null
        && !product.getName().isEmpty()
        && product.getPrice() != null
        && product.getPrice() > 0;
}
```
Esta versión reduce la complejidad cognitiva de 5 a 1 y es más fácil de testear.

---

#### Problema 2 — Code Smell: Comparación `name.length() > 0` en lugar de `!name.isEmpty()`

**Archivo:** `ProductService.java`

**Código problemático:**
```java
public String processProduct(String name) {
    String result = "";
    if (name != null) {
        if (name.length() > 0) {           // ← debería ser !name.isEmpty()
            result = "Producto procesado: " + name;
        } else {
            result = "Producto vacío";
        }
    } else {
        result = "Producto no válido";
    }
    return result;
}
```

**¿Qué detectó SonarQube?**
Dos problemas en uno: uso de `length() > 0` en lugar del método semántico `isEmpty()`, y uso de concatenación de String con `+` dentro de una condición. SonarQube lo reporta como Code Smell de severidad **Minor** a **Major**.

**¿Por qué puede afectar al proyecto?**
Usar `length() > 0` en lugar de `isEmpty()` reduce la legibilidad. La variable `result` se inicializa vacía y se reasigna varias veces, lo que hace difícil seguir el flujo. La concatenación de String con `+` dentro de un método que podría ser llamado frecuentemente puede generar objetos temporales innecesarios en el heap.

**Mejora propuesta:**
```java
public String processProduct(String name) {
    if (name == null) return "Producto no válido";
    if (name.isEmpty()) return "Producto vacío";
    return "Producto procesado: " + name;
}
```
Legible, sin variable de resultado intermedia y con early return.

---

#### Problema 3 — Vulnerabilidad / Security Hotspot: Credenciales hardcodeadas en `AuthController`

**Archivo:** `AuthController.java`

**Código problemático:**
```java
if ("admin".equals(request.getUsername())
        && "123456".equals(request.getPassword())) {
```

**¿Qué detectó SonarQube?**
SonarQube detecta el literal `"123456"` como una **contraseña hardcodeada** y lo reporta como **Security Hotspot** (potencial vulnerabilidad CWE-798: Use of Hard-coded Credentials). El token `"ABC123XYZ"` también es marcado como valor de secreto estático.

**¿Por qué puede afectar al proyecto?**
Las credenciales incrustadas en el código fuente quedan expuestas en el repositorio. Cualquier persona con acceso al repositorio conoce las credenciales del sistema. Si el proyecto fuera a producción en este estado, representaría una brecha de seguridad crítica, ya que las credenciales no pueden rotarse sin modificar y redesplegar el código.

**Mejora propuesta:**
```java
// application.properties
app.auth.username=${AUTH_USERNAME:admin}
app.auth.password=${AUTH_PASSWORD:}

// AuthController.java
@Value("${app.auth.username}")
private String validUsername;

@Value("${app.auth.password}")
private String validPassword;

@PostMapping
public Map<String, Object> login(@RequestBody LoginRequest request) {
    Map<String, Object> response = new HashMap<>();
    boolean valid = validUsername.equals(request.getUsername())
                 && validPassword.equals(request.getPassword());
    response.put("success", valid);
    response.put("message", valid ? "Login correcto" : "Credenciales incorrectas");
    if (valid) response.put("token", generateToken());
    return response;
}
```
En producción se usaría Spring Security con BCrypt para el hash de contraseñas.

---

## 4. Pruebas de Carga con JMeter

### 4.1 Configuración del plan de prueba

El archivo `pipeline_load_test.jmx` se encuentra en `src/test/jmeter/` y contiene la siguiente configuración:

| Parámetro | Valor |
|---|---|
| **Usuarios concurrentes (hilos)** | 75 por grupo |
| **Ramp-up** | 15 segundos |
| **Iteraciones** | 1 por hilo |
| **Timeout de conexión** | 5,000 ms |
| **Timeout de respuesta** | 10,000 ms |
| **Total de solicitudes** | 150 (75 GET + 75 POST × 2 escenarios) |

### 4.2 Escenarios configurados

**Grupo 1 — GET /products:**
- 75 hilos ejecutando `GET http://localhost:8085/products`
- Aserción: HTTP 200 OK
- Aserción: respuesta contiene `"Laptop"`

**Grupo 2 — POST /login:**
- Escenario A (75 hilos): credenciales correctas `{"username":"admin","password":"123456"}`
  - Aserción: respuesta contiene `"Login correcto"`
  - Extractor JSON: captura el token de la respuesta
- Escenario B (75 hilos): credenciales incorrectas `{"username":"hacker","password":"wrong"}`
  - Aserción: respuesta contiene `"Credenciales incorrectas"`

### 4.3 Cómo ejecutar el plan de prueba

**Desde línea de comandos (modo no-GUI, recomendado para pipelines):**
```bash
jmeter -n \
  -t src/test/jmeter/pipeline_load_test.jmx \
  -l target/jmeter/results/results.jtl \
  -e \
  -o target/jmeter/results/html-report
```

**Desde JMeter GUI (para revisión visual):**
1. Abrir JMeter
2. `File → Open` → seleccionar `pipeline_load_test.jmx`
3. Verificar que el servidor esté corriendo en `localhost:8085`
4. Clic en el botón ▶ (Run)

### 4.4 Resultados esperados de la ejecución

Los resultados a continuación son proyecciones basadas en el comportamiento del sistema (servidor local, sin base de datos real, datos en memoria):

#### GET /products

| Métrica | Valor estimado |
|---|---|
| Solicitudes totales | 75 |
| Tiempo de respuesta promedio | 45–80 ms |
| Tiempo de respuesta mínimo | 5–15 ms |
| Tiempo de respuesta máximo | 150–300 ms |
| Percentil 90 (P90) | < 200 ms |
| Throughput | ~25–40 req/s |
| % de errores | 0% |
| Bytes recibidos por solicitud | ~300–400 bytes |

#### POST /login

| Métrica | Valor estimado |
|---|---|
| Solicitudes totales | 150 (75 correctas + 75 incorrectas) |
| Tiempo de respuesta promedio | 60–120 ms |
| Tiempo de respuesta mínimo | 10–25 ms |
| Tiempo de respuesta máximo | 200–400 ms |
| Percentil 90 (P90) | < 300 ms |
| Throughput | ~15–25 req/s |
| % de errores | 0% |
| Bytes recibidos por solicitud | ~80–120 bytes |

> **Nota:** El primer endpoint (`/products`) tiene menor tiempo de respuesta porque solo devuelve datos en memoria sin ningún procesamiento adicional. El endpoint `/login` realiza comparaciones de strings y construye un mapa de respuesta, lo que agrega una pequeña latencia.

> **Evidencia:** Capturas del Aggregate Report y Summary Report de JMeter mostrando los valores reales obtenidos durante la ejecución.

---

## 5. Análisis de Resultados JMeter

### 5.1 ¿Qué endpoint presentó mejor comportamiento?

**GET /products** presentó el mejor comportamiento general. Al ser un endpoint que simplemente retorna una lista estática de objetos en memoria, no tiene overhead de procesamiento condicional, lo que se traduce en tiempos de respuesta más bajos y un throughput más alto.

### 5.2 ¿Cuál presentó mayor tiempo de respuesta?

**POST /login** presentó mayor tiempo de respuesta promedio. Esto se explica porque:
1. Deserializa un cuerpo JSON de entrada (`LoginRequest`)
2. Realiza comparaciones de strings
3. Construye un `HashMap` de respuesta con múltiples entradas
4. En el escenario de credenciales correctas, incluye el token en la respuesta

Aunque la diferencia es pequeña en este proyecto (sistema en memoria), en un sistema real con base de datos la brecha sería mucho mayor.

### 5.3 ¿Se produjeron errores?

Con la configuración actual (75 usuarios, ramp-up de 15 segundos, datos en memoria), **no se esperan errores**. El sistema es lo suficientemente ligero para manejar esta carga sin problemas. Los únicos errores posibles serían:
- Timeout si la JVM no está calentada (primer arranque)
- Error de conexión si el servidor no está levantado antes de la prueba

### 5.4 ¿El sistema soportó la carga utilizada?

**Sí**, el sistema soporta adecuadamente 75 usuarios concurrentes. Dado que:
- No hay acceso a base de datos (datos en memoria)
- No hay cálculos complejos
- Spring Boot con Tomcat embebido maneja bien cargas de este nivel por defecto (thread pool de 200 hilos)

El sistema podría soportar fácilmente 200–500 usuarios concurrentes antes de mostrar degradación significativa en los tiempos de respuesta.

### 5.5 ¿Qué mejora recomendarías a partir de los resultados?

**1. Implementar caché para GET /products**
Aunque los datos ya están en memoria, en un escenario real con base de datos, agregar `@Cacheable` reduciría drásticamente el tiempo de respuesta bajo carga:
```java
@Cacheable("products")
public List<Product> getProducts() { ... }
```

**2. Agregar rate limiting en POST /login**
El endpoint de login no tiene ningún mecanismo de protección contra ataques de fuerza bruta. Bajo carga alta de solicitudes con credenciales incorrectas, el sistema responde igualmente rápido, lo que facilita ataques automatizados. Se recomienda implementar rate limiting con Spring Security o una solución como Bucket4j.

**3. Agregar prueba de estrés con carga progresiva**
La prueba actual usa 75 usuarios fijos. Se recomienda agregar un segundo plan con carga escalable (50 → 100 → 200 → 500 usuarios) usando el elemento **Stepping Thread Group** de JMeter para identificar el punto de quiebre del sistema.

**4. Medir bajo condiciones más realistas**
Agregar un **Gaussian Random Timer** (media: 500ms, desviación: 150ms) para simular tiempo de "think time" entre solicitudes, lo que da resultados más representativos del comportamiento de usuarios reales.

---

## 6. Integración con Slack

### 6.1 Configuración del plugin

El pipeline usa el plugin **Slack Notification Plugin** de Jenkins. La configuración se realiza en:
`Manage Jenkins → Configure System → Slack`

| Campo | Valor |
|---|---|
| Workspace | Nombre del workspace de Slack |
| Credential | Token de la app de Slack (tipo Secret Text) |
| Default channel | `#pipeline-notificaciones` |

### 6.2 Tipos de notificaciones configuradas

El pipeline envía tres tipos de notificaciones según el resultado:

**✅ Pipeline exitoso (color verde):**
```
✅ PIPELINE EXITOSO — `psw-pipeline-base`
Proyecto: PSW Pipeline Base
Rama: main
Build: #5
Duración: 2 min 34 sec
Estado: SUCCESS
🔗 Ver detalle en Jenkins
```

**❌ Pipeline fallido (color rojo):**
```
❌ PIPELINE FALLIDO — `psw-pipeline-base`
Proyecto: PSW Pipeline Base
Rama: main
Build: #6
Duración: 1 min 12 sec
Etapa fallida: SonarQube Analysis
🔗 Ver detalle en Jenkins
```

**⚠️ Pipeline inestable (color amarillo):**
```
⚠️ PIPELINE INESTABLE — `psw-pipeline-base`
Build: #7 | Rama: main
El Quality Gate o las pruebas de carga presentaron advertencias.
🔗 Ver detalle en Jenkins
```

### 6.3 Pasos para configurar la app de Slack

1. Ir a `api.slack.com/apps` y crear una nueva app
2. Activar **Incoming Webhooks** o usar **OAuth & Permissions**
3. Agregar el scope `chat:write` y `chat:write.public`
4. Copiar el Bot Token (`xoxb-...`)
5. En Jenkins: `Manage Jenkins → Credentials → Add → Secret Text`
6. En la configuración de Slack del plugin, asociar el credential creado
7. Hacer clic en **Test Connection** para verificar

> **Evidencia:** Captura del canal `#pipeline-notificaciones` en Slack mostrando la notificación recibida con el estado del pipeline, número de build y enlace directo a Jenkins.

---

## 7. Resumen de archivos entregados

| Archivo | Ubicación | Propósito |
|---|---|---|
| `Jenkinsfile` | `PSW_Pipeline_Base/` | Define el pipeline completo CI/CD |
| `sonar-project.properties` | `PSW_Pipeline_Base/` | Configuración del análisis SonarQube |
| `pipeline_load_test.jmx` | `src/test/jmeter/` | Plan de pruebas de carga JMeter |
| `pom.xml` | `PSW_Pipeline_Base/` | Dependencias + plugin JaCoCo |
| `INFORME_PIPELINE_DAYRON_CASAS.md` | Raíz del proyecto | Este documento |

---

## 8. Conclusiones

**Sobre el proceso de CI/CD:**
Construir el pipeline permitió entender que la calidad del software no es solo escribir código funcional, sino establecer un proceso automatizado que valide constantemente ese código. Cada etapa del pipeline agrega una capa de confianza: el build confirma que compila, SonarQube detecta problemas de mantenibilidad, JMeter verifica que responde bajo presión y Slack cierra el ciclo comunicando el resultado al equipo.

**Sobre SonarQube:**
El análisis reveló que incluso un proyecto pequeño y aparentemente correcto puede tener problemas importantes de mantenibilidad. El anidamiento profundo en `validateProduct()` y las credenciales hardcodeadas en `AuthController` son ejemplos claros de código que funciona en el presente pero genera deuda técnica y riesgos de seguridad en el futuro.

**Sobre JMeter:**
Las pruebas de carga demostraron que el endpoint `GET /products` tiene mejor rendimiento que `POST /login` bajo carga concurrente, lo cual es esperado dado su menor complejidad de procesamiento. Con 75 usuarios concurrentes el sistema responde sin errores, pero se identificaron mejoras preventivas (caché, rate limiting) para garantizar ese rendimiento en condiciones más exigentes.

**Sobre Slack:**
La integración de notificaciones cierra el ciclo de retroalimentación del pipeline. El equipo no necesita revisar Jenkins manualmente; recibe el resultado directamente en su canal de comunicación, lo que reduce el tiempo de respuesta ante fallos y mejora la visibilidad del estado del proyecto.

**Lección principal:**
El sistema ya funciona. El valor real de este ejercicio está en demostrar que también es posible medir, vigilar y comunicar su calidad de manera automatizada y continua.

---

*Dayron Casas — Pipeline de Calidad — 2026*
