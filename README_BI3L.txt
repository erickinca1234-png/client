BI3L Client - mod Forge para Minecraft 1.8.9 (solo HUD: CPS, FPS, coordenadas)

REQUISITOS: JDK 17 (para correr Gradle) y JDK 8 instalados. Gradle se descarga solo con el wrapper.
COMPILAR (en esta carpeta):
    Windows:   gradlew build
    Linux/Mac: ./gradlew build
RESULTADO: build/libs/bi3l-1.0.0.jar  (ignora los jar de build/intermediates)
INSTALAR: copia ese jar a .minecraft/mods con Forge 1.8.9 (11.15.1.2318) instalado.
USO: Right Shift abre/cierra el menu.

SIN INSTALAR NADA (GitHub):
 1) Crea un repositorio en github.com y sube TODO el contenido de esta carpeta (incluida .github).
 2) Entra a la pestana "Actions", abre la ejecucion "Run Gradle Build" y espera ~5 min.
 3) Abajo, en "Artifacts", descarga "mod-jar": ahi esta bi3l-1.0.0.jar.
