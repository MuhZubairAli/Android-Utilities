# Android Utilities
A library of general-purpose utilities for Android applications, this library is part of Tablet based Data Collection System (TDS) project developed at the Pakistan Bureau of Statistics. It initially offers the following utilities and extensions
- Location Service (a foreground and bound service for location capturing)
- File Manager (for basic CRUD operation of File System)
- Compression / Decompression (ZipManager)
- Custom Application, Activity Classe (CustomActivity provides simpler way to handle permissions across all supported API levels)
- UXToolkit (basic UI/UX-related functionality, it is primarily a wrapper class)
- Static and System Utility
- Wrapper class for Http requests handling (uses Volley internally)
- Other utility classes (Theme and Font Management)

## Installation
In order to use this library in you android application follow following steps

### Step 1: Get personal access token from github with read:packages scope

  1. Log in to your GitHub account and goto **Settings** (click on profile photo at top right corner and from the menu click *Settings*)

  ![image](https://github.com/MuhZubairAli/Android-Utilities/assets/22114590/efc2924e-7537-4650-8328-482cb9fd64b3)

  3. In the left sidebar, click  **Developer settings**.
  4. In the left sidebar, click **Personal Access Tokens**.
  5. Click Generate new token.
  6. In the "Note" field, give your token a descriptive name like *"Token for reading github packages"*.
  7. To give your token an expiration, setting it to *No expiration* is fine because we will use it to download packages.
  8. Select the scope to *read:packages* as shown in below picture
  
  ![image](https://github.com/MuhZubairAli/Android-Utilities/assets/22114590/e70780c3-c26c-4846-af63-be2632c607e1)

  9. Click Generate token.

### Step 2: Add maven repository for dependency downloads
Depending on your gradle version add repository for all projects, from gradle 7.3.3 add a repository as follow

```
maven {
    url uri('https://maven.pkg.github.com/MuhZubairAli/*')
    credentials {
        username 'YourGithubUsername'
        password 'PersonalAccessToken'
    }
}
```

*If you are facing any issues in generating token you use below username and token*

```
maven {
    url uri('https://maven.pkg.github.com/MuhZubairAli/*')
    credentials {
        username 'MuhZubairAli'
        password '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'
    }
}
```

### Step 3: Add dependencies into build.gradle file
If you want to use libray in your application add dependency in your ROOT_DIRECTORY/app/build.gradle or if you intend to use it in any module then add it to ROOT_DIRECTORY/MODULE_DIRECTORY/build.gradle

```
dependencies {
  implementation 'com.github.muhzubairali:utils:$VERSION'
  // other dependencies
}
```
To get the latest version click package, the latest package details could be found in POM (or in the title)


![image](https://github.com/MuhZubairAli/Android-Utilities/assets/22114590/5282a33f-4c10-405d-b737-ddd5fc946c08)

