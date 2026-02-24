# 上传测试网页实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 创建独立的HTML测试网页，用于模拟前端参与完整的三步文件上传流程（prepare → OSS直传 → complete）

**架构：** 纯静态HTML文件 + 原生JavaScript，通过fetch API调用后端RESTful接口，后端添加CORS配置支持跨域访问

**技术栈：** HTML5, CSS,原生JavaScript (Fetch API), Spring Boot CORS配置

---

### Task 1: 添加CORS配置支持

**Files:**
- Modify: `src/main/java/top/enderliquid/audioflow/common/config/WebMvcConfig.java`

**Step 1: 读取现有WebMvcConfig文件**

确认当前配置内容，了解现有拦截器注册代码

**Step 2: 添加CORS映射配置**

在 `WebMvcConfig` 类中添加 `addCorsMappings` 方法重写：

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
}
```

注意：在现有 `addInterceptors` 方法之前添加此方法

**Step 3: 编译项目验证配置**

Run: `./mvnw clean compile`
Expected:编译成功，无错误

**Step 4: 运行测试验证CORS配置**

Run: `./mvnw test`
Expected:所有测试通过

**Step 5: 提交CORS配置**

```bash
git add src/main/java/top/enderliquid/audioflow/common/config/WebMvcConfig.java
git commit -m "添加CORS配置以支持跨域请求"
```

---

### Task 2: 创建HTML测试网页的基础结构

**Files:**
- Create: `upload-test.html`

**Step 1: 创建HTML文件基础结构**

创建文件 `upload-test.html`，包含：
- HTML5文档类型声明
- 基本的meta标签和标题
- 内嵌CSS样式（基础布局）
- 页面标题和简单的分段区域

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AudioFlow 上传测试</title>
    <style>
        body {
            font-family: monospace;
            max-width: 800px;
            margin: 50px auto;
            padding: 20px;
            background: #f5f5f5;
        }
        .section {
            background: white;
            padding: 20px;
            margin-bottom: 20px;
            border: 1px solid #ddd;
        }
        h2 {
            margin-top: 0;
            color: #333;
        }
        .input-group {
            margin-bottom: 15px;
        }
        label {
            display: block;
            margin-bottom: 5px;
            color: #555;
        }
        input, select {
            width: 100%;
            padding: 8px;
            border: 1px solid #ccc;
            box-sizing: border-box;
        }
        button {
            padding: 10px 20px;
            background: #007bff;
            color: white;
            border: none;
            cursor: pointer;
            margin-top: 10px;
        }
        button:hover {
            background: #0056b3;
        }
        .status {
            margin-top: 10px;
            padding: 10px;
            background: #f8f9fa;
            border-radius: 4px;
        }
        .step {
            padding: 8px;
            margin: 5px 0;
        }
        .step.pending {
            color: #999;
        }
        .step.in-progress {
            color: #ff9800;
            animation: blink 1s infinite;
        }
        .step.success {
            color: #28a745;
        }
        .step.failure {
            color: #dc3545;
        }
        @keyframes blink {
            50% { opacity: 0.5; }
        }
        .result {
            margin-top: 10px;
            padding: 10px;
            background: #e9ecef;
            border-radius: 4px;
        }
        #error {
            color: #dc3545;
            margin-top: 10px;
        }
    </style>
</head>
<body>
    <div class="section">
        <h2>后端地址</h2>
        <div class="input-group">
            <label>Base URL:</label>
            <input type="text" id="baseUrl" value="http://localhost:8080" placeholder="http://localhost:8081">
            <button onclick="testConnection()">连接测试</button>
        </div>
        <div id="connectionStatus" class="status">未连接</div>
    </div>
</body>
</html>
```

**Step 2: 在浏览器中打开HTML文件验证基础布局**

Run: 使用浏览器打开 `upload-test.html`
Expected:页面正常显示后端地址配置区域，样式正确

---

### Task 3: 实现登录功能

**Files:**
- Modify: `upload-test.html`

**Step 1: 在HTML中添加登录区域**

在 `</body>` 标签之前添加登录表单：

```html
<div class="section">
    <h2>登录</h2>
    <div class="input-group">
        <label>邮箱:</label>
        <input type="email" id="email" placeholder="user@example.com">
    </div>
    <div class="input-group">
        <label>密码:</label>
        <input type="password" id="password" placeholder="password">
    </div>
    <button onclick="login()">登录</button>
    <div id="loginStatus" class="status">未登录</div>
</div>
```

**Step 2: 添加登录JavaScript函数**

在 `<script>` 标签中（放在 `</body>` 之前）添加：

```javascript
let satoken = null;

function testConnection() {
    const baseUrl = document.getElementById('baseUrl').value;
    const status = document.getElementById('connectionStatus');
    status.innerHTML = '连接中...';
    status.className = 'status';
    
    fetch(`${baseUrl}/api/sessions`, {
        method: 'OPTIONS'
    }).then(response => {
        if (response.ok || response.status === 405) {
            status.innerHTML = '连接成功';
            status.className = 'status success';
        } else {
            status.innerHTML = '连接失败';
            status.className = 'status failure';
        }
    }).catch(error => {
        status.innerHTML = `连接失败: ${error.message}`;
        status.className = 'status failure';
    });
}

function login() {
    const baseUrl = document.getElementById('baseUrl').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const status = document.getElementById('loginStatus');
    
    if (!email || !password) {
        status.innerHTML = '请填写邮箱和密码';
        status.className = 'status failure';
        return;
    }
    
    status.innerHTML = '登录中...';
    status.className = 'status';
    
    fetch(`${baseUrl}/api/sessions`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            email: email,
            password: password
        }),
        credentials: 'include'
    }).then(response => {
        return response.json();
    }).then(data => {
        if (data.success) {
            satoken = document.cookie.match(/satoken=([^;]+)/)?.[1];
            status.innerHTML = `登录成功 - ${data.data.name}`;
            status.className = 'status success';
        } else {
            status.innerHTML = `登录失败: ${data.message}`;
            status.className = 'status failure';
        }
    }).catch(error => {
        status.innerHTML = `登录失败: ${error.message}`;
        status.className = 'status failure';
    });
}
```

**Step 3: 刷新浏览器页面验证登录功能**

在浏览器中刷新 `upload-test.html`
Expected:登录表单显示正常，点击登录后可以调用后端API

**Step 4: 提交HTML文件**

```bash
git add upload-test.html
git commit -m "创建上传测试网页基础结构和登录功能"
```

---

### Task 4: 实现上传表单区域

**Files:**
- Modify: `upload-test.html`

**Step 1: 在HTML中添加上传区域**

在登录区域之后添加上传表单：

```html
<div class="section">
    <h2>上传</h2>
    <div class="input-group">
        <label>歌曲名称:</label>
        <input type="text" id="songName" placeholder="歌曲名称（可选）">
    </div>
    <div class="input-group">
        <label>描述:</label>
        <input type="text" id="description" placeholder="歌曲描述（可选）">
    </div>
    <div class="input-group">
        <label>文件类型:</label>
        <select id="fileType">
            <option value="audio/mpeg">MP3 (audio/mpeg)</option>
            <option value="audio/mp3">MP3 (audio/mp3)</option>
            <option value="audio/wav">WAV (audio/wav)</option>
            <option value="audio/ogg">OGG (audio/ogg)</option>
            <option value="audio/flac">FLAC (audio/flac)</option>
        </select>
    </div>
    <div class="input-group">
        <label>选择文件:</label>
        <input type="file" id="fileInput" accept=".mp3,.wav,.ogg,.flac">
        <div id="selectedFile" style="margin-top: 5px; color: #666;">未选择文件</div>
    </div>
    <button id="uploadBtn" onclick="startUpload()">开始上传</button>
</div>
```

**Step 2: 添加文件选择监听器和上传状态区域**

在上传区域之后添加上传进度和结果区域，并在 `<script>` 中添加文件选择监听：

```html
<div class="section">
    <h2>上传进度</h2>
    <div id="step1" class="step pending">1. 登录</div>
    <div id="step2" class="step pending">2. 准备上传</div>
    <div id="step3" class="step pending">3. 上传到OSS</div>
    <div id="step4" class="step pending">4. 完成验证</div>
    <div id="error" style="display: none;"></div>
</div>

<div class="section">
    <h2>结果详情</h2>
    <div id="result" class="result">
        <div>歌曲ID: -</div>
        <div>文件名: -</div>
        <div>时长: -</div>
        <div>大小: -</div>
    </div>
</div>
```

在 `<script>` 中添加文件选择监听器：

```javascript
const fileInput = document.getElementById('fileInput');
fileInput.addEventListener('change', function(e) {
    const file = e.target.files[0];
    const selectedFileDiv = document.getElementById('selectedFile');
    if (file) {
        selectedFileDiv.innerHTML = `已选: ${file.name} (${(file.size / 1024 / 1024).toFixed(2)} MB)`;
    } else {
        selectedFileDiv.innerHTML = '未选择文件';
    }
});
```

**Step 3: 刷新浏览器验证上传表单显示**

在浏览器中刷新页面
Expected:上传表单正确显示，文件选择监听器工作正常

**Step 4: 提交HTML文件**

```bash
git add upload-test.html
git commit -m "添加上传表单区域"
```

---

### Task 5: 实现准备上传功能

**Files:**
- Modify: `upload-test.html`

**Step 1: 在JavaScript中添加准备上传函数**

在 `<script>` 中添加 `prepareUpload` 函数和状态更新辅助函数：

```javascript
function updateStep(stepId, statusClass, text) {
    const step = document.getElementById(stepId);
    step.className = `step ${statusClass}`;
    if (text) {
        step.innerHTML = text;
    }
}

function showError(message) {
    const errorDiv = document.getElementById('error');
    errorDiv.style.display = 'block';
    errorDiv.innerHTML = `错误: ${message}`;
}

function hideError() {
    document.getElementById('error').style.display = 'none';
}

async function prepareUpload() {
    const baseUrl = document.getElementById('baseUrl').value;
    const songName = document.getElementById('songName').value;
    const description = document.getElementById('description').value;
    const fileType = document.getElementById('fileType').value;
    
    if (!satoken) {
        throw new Error('请先登录');
    }
    
    updateStep('step2', 'in-progress', '2. 准备上传中...');
    
    const response = await fetch(`${baseUrl}/api/songs/prepare`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
            mimeType: fileType,
            name: songName || null,
            description: description || null
        })
    });
    
    const data = await response.json();
    
    if (!data.success) {
        throw new Error(data.message || '准备上传失败');
    }
    
    updateStep('step2', 'success', '2. 准备上传成功');
    return data.data;
}
```

**Step 2: 添加startUpload函数框架**

在 `<script>` 中添加 `startUpload` 函数的开始部分：

```javascript
async function startUpload() {
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];
    
    hideError();
    updateStep('step1', 'pending', '1. 登录');
    updateStep('step2', 'pending', '2. 准备上传');
    updateStep('step3', 'pending', '3. 上传到OSS');
    updateStep('step4', 'pending', '4. 完成验证');
    
    document.getElementById('result').innerHTML = `
        <div>歌曲ID: -</div>
        <div>文件名: -</div>
        <div>时长: -</div>
        <div>大小: -</div>
    `;
    
    if (!file) {
        showError('请选择文件');
        return;
    }
    
    try {
        updateStep('step1', 'success', '1. 登录成功');
        
        const prepareResult = await prepareUpload();
        
    } catch (error) {
        showError(error.message);
    }
}
```

**Step 3: 刷新浏览器验证准备上传功能**

在浏览器中刷新页面，启动后端服务，尝试上传
Expected:点击"开始上传"后，prepare步骤能够成功调用后端API

**Step 4: 提交HTML文件**

```bash
git add upload-test.html
git commit -m "实现准备上传功能"
```

---

### Task 6: 实现OSS直接上传功能

**Files:**
- Modify: `upload-test.html`

**Step 1: 在JavaScript中添加OSS上传函数**

在 `<script>` 中添加 `uploadToOSS` 函数：

```javascript
async function uploadToOSS(uploadUrl, file, mimeType) {
    updateStep('step3', 'in-progress', `3. 上传到OSS中... (0%)`);
    
    const response = await fetch(uploadUrl, {
        method: 'PUT',
        headers: {
            'Content-Type': mimeType
        },
        body: file
    });
    
    if (!response.ok) {
        throw new Error(`OSS上传失败: ${response.status}`);
    }
    
    updateStep('step3', 'success', '3. 上传到OSS成功');
}
```

**Step 2: 更新startUpload函数调用OSS上传**

修改 `startUpload` 函数，在 prepareUpload 调用后添加OSS上传：

```javascript
async function startUpload() {
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];
    const fileType = document.getElementById('fileType').value;
    
    hideError();
    updateStep('step1', 'pending', '1. 登录');
    updateStep('step2', 'pending', '2. 准备上传');
    updateStep('step3', 'pending', '3. 上传到OSS');
    updateStep('step4', 'pending', '4. 完成验证');
    
    document.getElementById('result').innerHTML = `
        <div>歌曲ID: -</div>
        <div>文件名: -</div>
        <div>时长: -</div>
        <div>大小: -</div>
    `;
    
    if (!file) {
        showError('请选择文件');
        return;
    }
    
    try {
        updateStep('step1', 'success', '1. 登录成功');
        
        const prepareResult = await prepareUpload();
        
        await uploadToOSS(prepareResult.uploadUrl, file, fileType);
        
    } catch (error) {
        showError(error.message);
    }
}
```

**Step 3: 刷新浏览器验证OSS上传功能**

在浏览器中刷新页面，启动后端服务，尝试完整上传
Expected:OSS上传步骤能够成功执行

**Step 4: 提交HTML文件**

```bash
git add upload-test.html
git commit -m "实现OSS直接上传功能"
```

---

### Task 7: 实现完成上传功能

**Files:**
- Modify: `upload-test.html`

**Step 1: 在JavaScript中添加完成上传函数**

在 `<script>` 中添加 `completeUpload` 函数：

```javascript
async function completeUpload(songId) {
    const baseUrl = document.getElementById('baseUrl').value;
    
    updateStep('step4', 'in-progress', '4. 完成验证中...');
    
    const response = await fetch(`${baseUrl}/api/songs/complete`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
            songId: songId
        })
    });
    
    const data = await response.json();
    
    if (!data.success) {
        throw new Error(data.message || '完成上传失败');
    }
    
    updateStep('step4', 'success', '4. 完成验证成功');
    return data.data;
}

function formatDuration(ms) {
    if (!ms) return '-';
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
}

function formatSize(bytes) {
    if (!bytes) return '-';
    const mb = bytes / 1024 / 1024;
    return `${mb.toFixed(2)} MB`;
}

function displayResult(song) {
    const resultDiv = document.getElementById('result');
    resultDiv.innerHTML = `
        <div>歌曲ID: ${song.id}</div>
        <div>文件名: ${song.fileName || song.id + '.mp3'}</div>
        <div>时长: ${formatDuration(song.duration)}</div>
        <div>大小: ${formatSize(song.size)}</div>
        <div>上传者: ${song.uploaderName}</div>
        <div>状态: ${song.status}</div>
    `;
}
```

**Step 2: 更新startUpload函数调用完成上传**

修改 `startUpload` 函数，在 uploadToOSS 调用后添加完成上传：

```javascript
async function startUpload() {
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];
    const fileType = document.getElementById('fileType').value;
    
    hideError();
    updateStep('step1', 'pending', '1. 登录');
    updateStep('step2', 'pending', '2. 准备上传');
    updateStep('step3', 'pending', '3. 上传到OSS');
    updateStep('step4', 'pending', '4. 完成验证');
    
    document.getElementById('result').innerHTML = `
        <div>歌曲ID: -</div>
        <div>文件名: -</div>
        <div>时长: -</div>
        <div>大小: -</div>
    `;
    
    if (!file) {
        showError('请选择文件');
        return;
    }
    
    try {
        updateStep('step1', 'success', '1. 登录成功');
        
        const prepareResult = await prepareUpload();
        
        await uploadToOSS(prepareResult.uploadUrl, file, fileType);
        
        const completeResult = await completeUpload(prepareResult.id);
        
        displayResult(completeResult);
        
    } catch (error) {
        showError(error.message);
    }
}
```

**Step 3: 刷新浏览器验证完整上传流程**

在浏览器中刷新页面，启动后端服务，使用真实的OSS配置测试完整上传流程
Expected:四个步骤全部成功，结果显示正确的歌曲信息

**Step 4: 提交HTML文件**

```bash
git add upload-test.html
git commit -m "实现完成上传功能和结果展示"
```

---

### Task 8: 测试完整上传流程

**Files:**
- `upload-test.html`

**Step 1: 准备测试环境**

确认后端服务已启动：
```bash
./mvnw spring-boot:run
```

确认OSS配置正确（检查 application.properties）

**Step 2: 测试正常上传流程**

在浏览器中打开 `upload-test.html`
1. 配置后端地址
2. 输入正确的邮箱和密码，点击登录
3. 填写歌曲信息，选择MP3文件
4. 点击"开始上传"
5. 验证所有步骤成功完成
6. 验证结果区域显示正确的歌曲信息

Expected: 所有步骤显示成功状态（绿色），结果显示歌曲ID、时长、大小等

**Step 3: 测试错误场景**

登录失败：
1. 输入错误的密码
2. 点击登录
Expected: 显示"登录失败"错误消息

文件未选择：
1. 不选择文件直接点击"开始上传"
Expected: 显示"请选择文件"错误消息

未登录上传：
1. 清除登录状态或刷新页面
2. 直接点击"开始上传"
Expected: 显示"请先登录"错误消息

**Step 4: 测试不同文件类型**

分别选择以下文件类型测试：
- MP3文件
- WAV文件
- OGG文件
- FLAC文件

Expected: 所有支持的文件类型都能正常上传

**Step 5: 确认功能完整性**

验证以下功能点：
- 登录状态正确显示
- 文件选择后显示文件名和大小
- 四个上传步骤的状态正确更新
- 上传成功后结果正确显示
- 错误信息正确显示

**Step 6: 提交最终的HTML文件**

```bash
git add upload-test.html
git commit -m "完成上传测试网页开发和测试"
```

---

## 验收标准

- [ ] 后端CORS配置已添加并正常工作
- [ ] 上传测试网页可以独立运行
- [ ] 登录功能正常工作，可以获取satoken
- [ ] 准备上传功能正常，可以获取预签名URL和songId
- [ ] OSS直传功能正常，文件可以成功上传到OSS
- [ ] 完成上传功能正常，可以获取完整的歌曲信息
- [ ] 四个上传步骤的状态可以正确显示
- [ ] 错误处理正常，可以显示详细的错误信息
- [ ] 结果详情可以正确展示歌曲信息
- [ ] 所有测试场景（正常流程、错误场景、不同文件类型）都已验证通过

## 注意事项

1. 上传测试网页需要后端服务运行在配置的地址
2. 需要正确的OSS配置才能完成完整的上传流程
3. 网页使用原生JavaScript，无需任何外部依赖
4. satoken cookie会自动由浏览器管理
5. CORS配置允许跨域请求，用于测试网页访问后端API
