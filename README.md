# API & Impl Module Generator  

<img src="https://placehold.co/600x400?text=API+Impl+Generator" alt="Screenshot of plugin interface showing template selection for API and Impl modules" width="300"/>

A powerful Android Studio plugin for rapid generation of API contracts and their implementations using customizable templates.

## 🔥 Features  

- **One-click generation** of `:api` and `:impl` module structure  
- **Configurable templates** for different architectures (clean, MVP, MVVM etc.)  
- **Automatic Gradle configuration** with proper dependency setup  
- **Team-friendly conventions** ensuring project consistency  
- **Smart code generation** with best-practice patterns  

## 🚀 Quick Start  

1. **Install the plugin**:  
   - Via Android Studio: `Settings → Plugins → Marketplace → Search "API & Impl Module Generator"`  
   - Or download [latest release](#)

2. **Create new module pair**:  
   <img src="https://placehold.co/400x200?text=Right-click+module+→+New+→+API+Impl+Pair" alt="Menu demonstration showing creation flow" width="400"/>  

3. **Configure**:  
   ```kotlin
   // Example of generated API module structure
   myfeature/
   ├── api/
   │   └── src/main/java/com/app/myfeature/api/
   │       └── MyFeatureContract.kt
   └── impl/
       └── src/main/java/com/app/myfeature/
           └── MyFeatureImpl.ktiiшш
