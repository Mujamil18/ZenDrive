document.addEventListener('DOMContentLoaded', function() {
    // State management
    let files = [];
    let currentCategory = 'home';
    let editor = null;
    let uploadedFiles = new Map();

    // File type categories
    const fileCategories = {
        javascript: ['.js', '.jsx', '.ts', '.tsx'],
        html: ['.html', '.htm'],
        css: ['.css', '.scss', '.sass'],
        python: ['.py'],
        java: ['.java'],
        cpp: ['.cpp', '.hpp'],
        c: ['.c', '.h'],
        csharp: ['.cs']
    };

    // DOM Elements
    const fileUpload = document.getElementById('file-input');
    const filesGrid = document.getElementById('preview-container');
    const categoryTitle = document.getElementById('category-title');
    const modal = document.getElementById('modal');
    const modalContent = document.getElementById('modal-content');
    const closeBtn = document.querySelector('.close-btn');
    const searchInput = document.getElementById('searchInput');
    const menuItems = document.querySelectorAll('.menu');

    // Close modal when clicking close button or outside
    closeBtn.addEventListener('click', () => {
        modal.style.display = 'none';
        modalContent.innerHTML = '';
    });

    window.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.style.display = 'none';
            modalContent.innerHTML = '';
        }
    });

    // Category handling
    menuItems.forEach(item => {
        item.addEventListener('click', function() {
            menuItems.forEach(i => i.classList.remove('active'));
            this.classList.add('active');
            currentCategory = this.dataset.category;
            categoryTitle.textContent = currentCategory === 'home' ? 'All Files' : currentCategory;

            if (currentCategory === 'code') {
                displayCodeFiles();
            } else {
                filesGrid.style.display = 'grid';
                const codeContainer = document.getElementById('code-files-container');
                if (codeContainer) {
                    codeContainer.style.display = 'none';
                }
                filterFiles();
            }
        });
    });

    // File upload handling
    fileUpload.addEventListener('change', function(event) {
        const formData = new FormData();
        const files = event.target.files;

        for (let file of files) {
            formData.append('file', file);

            // Handle code files
            const extension = '.' + file.name.split('.').pop().toLowerCase();
            let fileType = null;

            for (const [type, extensions] of Object.entries(fileCategories)) {
                if (extensions.includes(extension)) {
                    fileType = type;
                    break;
                }
            }

            if (fileType) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    uploadedFiles.set(file.name, {
                        name: file.name,
                        content: e.target.result,
                        type: fileType
                    });
                    if (currentCategory === 'code') {
                        displayCodeFiles();
                    }
                };
                reader.readAsText(file);
            }
        }

        // Send to upload servlet
        fetch('/WorkdrivePlusPlus/upload', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            console.log("Files uploaded successfully");
            displayUploadedFiles(Array.from(files));
        })
        .catch(error => {
            console.error('Error uploading files:', error);
        });
    });

    function displayCodeFiles() {
        filesGrid.style.display = 'none';
        const container = document.getElementById('code-files-container') || createCodeFilesContainer();
        container.style.display = 'grid';
        container.innerHTML = '';

        uploadedFiles.forEach(file => {
            const fileCard = createCodeFileCard(file);
            container.appendChild(fileCard);
        });
    }

    function createCodeFilesContainer() {
        const container = document.createElement('div');
        container.id = 'code-files-container';
        container.className = 'code-files-grid';
        document.querySelector('.content').appendChild(container);
        return container;
    }

    function createCodeFileCard(file) {
        const card = document.createElement('div');
        card.className = 'code-file-card';

        const icon = getFileIcon(file.type);

        card.innerHTML = `
            <div class="code-file-icon">
                <i class="${icon}"></i>
            </div>
            <div class="code-file-info">
                <h3>${file.name}</h3>
                <p>${file.type.toUpperCase()}</p>
            </div>
        `;

        card.addEventListener('click', () => openCodePlayground(file));
        return card;
    }

    function getFileIcon(type) {
        const icons = {
            javascript: 'fab fa-js',
            html: 'fab fa-html5',
            css: 'fab fa-css3-alt',
            python: 'fab fa-python',
            java: 'fab fa-java',
            cpp: 'fas fa-file-code',
            c: 'fas fa-file-code',
            csharp: 'fas fa-file-code'
        };
        return icons[type] || 'fas fa-file-code';
    }

    function openCodePlayground(file) {
        modalContent.innerHTML = `
            <div class="code-playground">
                <div class="playground-header">
                    <div class="file-info">
                        <i class="${getFileIcon(file.type)}"></i>
                        <span>${file.name}</span>
                    </div>
                    <div class="playground-actions">
                        <button id="save-code" class="save-button">
                            <i class="fas fa-save"></i> Save
                        </button>
                        <button id="run-code" class="run-button">
                            <i class="fas fa-play"></i> Run
                        </button>
                    </div>
                </div>
                <div class="playground-container">
                    <div class="editor-container">
                        <div id="code-editor"></div>
                    </div>
                    <div class="output-container">
                        <div class="output-header">
                            <h3>Output</h3>
                            <button id="clear-output" class="clear-button">
                                <i class="fas fa-trash"></i> Clear
                            </button>
                        </div>
                        <div id="code-output"></div>
                    </div>
                </div>
            </div>
        `;

        modal.style.display = 'flex';
        initializeCodeEditor(file);
    }

    function initializeCodeEditor(file) {
        const modeMap = {
            javascript: 'javascript',
            html: 'xml',
            css: 'css',
            python: 'python',
            java: 'text/x-java',
            cpp: 'text/x-c++src',
            c: 'text/x-csrc',
            csharp: 'text/x-csharp'
        };

        editor = CodeMirror(document.getElementById('code-editor'), {
            mode: modeMap[file.type] || 'javascript',
            theme: 'monokai',
            lineNumbers: true,
            autoCloseBrackets: true,
            matchBrackets: true,
            indentUnit: 4,
            tabSize: 4,
            lineWrapping: true,
            value: file.content || ''
        });

        const runButton = document.getElementById('run-code');
        const saveButton = document.getElementById('save-code');
        const outputDiv = document.getElementById('code-output');
        const clearButton = document.getElementById('clear-output');

        runButton.addEventListener('click', () => {
            const code = editor.getValue();
            outputDiv.innerHTML = '';

            try {
                if (file.type === 'javascript') {
                    const originalLog = console.log;
                    const logs = [];
                    console.log = function() {
                        logs.push(Array.from(arguments).join(' '));
                        originalLog.apply(console, arguments);
                    };
                    const result = eval(code);
                    console.log = originalLog;

                    if (logs.length > 0) {
                        outputDiv.innerHTML += logs.join('\n') + '\n';
                    }
                    if (result !== undefined) {
                        outputDiv.innerHTML += '=> ' + result;
                    }
                } else if (file.type === 'html') {
                    outputDiv.innerHTML = code;
                } else if (file.type === 'css') {
                    const style = document.createElement('style');
                    style.textContent = code;
                    outputDiv.innerHTML = '';
                    outputDiv.appendChild(style);
                    outputDiv.innerHTML += '<div class="css-preview">CSS Preview Area</div>';
                } else {
                    outputDiv.innerHTML = `Code execution for ${file.type} is not supported in the browser.\nConsider using a backend service for compilation and execution.`;
                }
            } catch (error) {
                outputDiv.innerHTML = `Error: ${error.message}`;
            }
        });

        saveButton.addEventListener('click', () => {
            const newContent = editor.getValue();
            const blob = new Blob([newContent], { type: 'text/plain' });
            const newFile = new File([blob], file.name, { type: 'text/plain' });
            const formData = new FormData();
            formData.append('file', newFile);

            fetch('/WorkdrivePlusPlus/upload', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                outputDiv.innerHTML = 'File saved successfully!';
                uploadedFiles.set(file.name, {
                    ...file,
                    content: newContent
                });
            })
            .catch(error => {
                outputDiv.innerHTML = `Error saving file: ${error.message}`;
            });
        });

        clearButton.addEventListener('click', () => {
            outputDiv.innerHTML = '';
        });
    }

    function displayUploadedFiles(files) {
        for (let file of files) {
            const fileCard = createFileCard(file);
            filesGrid.appendChild(fileCard);
        }
    }

    function createFileCard(file) {
            const fileCard = document.createElement('div');
            fileCard.className = 'file-card';
            fileCard.setAttribute('data-name', file.name);
            console.log('Created file card with name:', file.name);  // Check if data-name is set
            const preview = document.createElement('div');
            preview.className = 'file-preview';

            // Add content to preview based on file type
            if (file.type.startsWith('image/')) {
                const img = document.createElement('img');
                img.src = URL.createObjectURL(file);
                img.className = 'thumbnail';
                preview.appendChild(img);
            } else if (file.type.startsWith('video/')) {
                const video = document.createElement('video');
                video.src = URL.createObjectURL(file);
                video.className = 'thumbnail';
                video.muted = true;
                video.playsInline = true;
                video.loop = true;
                video.autoplay = true;
                preview.appendChild(video);
            } else {
                const icon = document.createElement('div');
                icon.className = 'file-icon';
                icon.innerHTML = getFileTypeIcon(file.type);
                preview.appendChild(icon);
            }

            // Add the preview div to the fileCard
            fileCard.appendChild(preview);

            // Attach event listener to the fileCard itself (instead of using querySelectorAll)
                    fileCard.addEventListener('click', function(event) {
                        updatePreviewBar(event);  // Pass the event to the function
                    });

            // Optionally, if you want to handle a click event inside the preview as well
                    preview.addEventListener('click', function(event) {
                        updatePreviewBar(event);  // Pass the event to the function
                    });

            // Append the fileCard to the container (assumed to be pre-existing in your DOM)
            document.getElementById('file-container').appendChild(fileCard);
        }

        function updatePreviewBar(event) {
            let fileCard = event.target.closest('.file-card');  // This ensures you get the correct parent element
            console.log('Event target:', event.target);  // Check the actual target of the event
                let fileCard = event.target.closest('.file-card'); // Get the closest .file-card
                if (!fileCard) {
                    console.error("Error: No file card found");
                    return;
                }

                let fileClicked = fileCard.getAttribute('data-name');
                console.log('File clicked:', fileClicked); // Log the file name

                // Send a POST request to the servlet with the file name
                fetch('/WorkdrivePlusPlus/fetchMetadata', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ fileName: fileClicked })  // Send the file name in the request body
                })
                .then(response => response.json())  // Parse the response as JSON
                .then(data => {
                    console.log('Received metadata:', data);

                    // Assuming the response contains file metadata, display it in the preview bar
                    const metadataContainer = document.getElementById('metadata-container');
                    if (data.error) {
                        metadataContainer.innerHTML = `<p>Error: ${data.error}</p>`;
                    } else {
                        // Display the metadata (you can adjust the structure based on the actual response)
                        metadataContainer.innerHTML = `
                            <h3>Metadata for ${fileClicked}</h3>
                            <p>File ID: ${data.id}</p>
                            <p>File Name: ${data.name}</p>
                            <p>File Type: ${data.type}</p>
                            <p>File Size: ${data.size} bytes</p>
                        `;
                    }
                })
                .catch(error => {
                    console.error('Error fetching metadata:', error);
                });
            }

    // Update the openFilePreview function for video files
    function openFilePreview(file) {
        modalContent.innerHTML = '';

        if (file.type.startsWith('image/')) {
            modalContent.innerHTML = `
                <img src="${URL.createObjectURL(file)}" alt="Preview" class="modal-image">
            `;
        } else if (file.type.startsWith('video/')) {
            modalContent.innerHTML = `
                <div class="video-notes-container">
                    <div class="video-controls">
                        <button class="generate-notes-btn" onclick="generateVideoNotes('${URL.createObjectURL(file)}')">
                            <i class="fas fa-file-alt"></i> Generate Notes
                        </button>
                    </div>
                    <div class="content-container">
                        <div class="video-container">
                            <video src="${URL.createObjectURL(file)}" controls class="modal-video" autoplay></video>
                        </div>
                        <div class="notes-container">
                            <div class="notes-header">
                                <h3>Generated Notes</h3>
                                <button class="copy-btn" onclick="copyNotes()">
                                    <i class="fas fa-copy"></i> Copy
                                </button>
                            </div>
                            <div class="notes-content"></div>
                        </div>
                    </div>
                </div>`;
        } else if (file.type.startsWith('audio/')) {
            modalContent.innerHTML = `
                <div class="audio-player">
                    <audio src="${URL.createObjectURL(file)}" controls></audio>
                    <div class="audio-info">
                        <i class="fas fa-music"></i>
                        <span>${file.name}</span>
                    </div>
                </div>`;
        } else if (file.type === 'application/pdf') {
            modalContent.innerHTML = `
                <iframe src="${URL.createObjectURL(file)}" class="modal-iframe"></iframe>`;
        } else if (file.type.startsWith('text/')) {
            const reader = new FileReader();
            reader.onload = function(e) {
                modalContent.innerHTML = `
                    <div class="text-preview">
                        <pre>${e.target.result}</pre>
                    </div>`;
            };
            reader.readAsText(file);
        } else {
            modalContent.innerHTML = `
                <div class="file-preview-fallback">
                    ${getFileTypeIcon(file.type)}
                    <p>${file.name}</p>
                    <a href="${URL.createObjectURL(file)}" download="${file.name}" class="download-button">
                        <i class="fas fa-download"></i> Download
                    </a>
                </div>`;
        }

        modal.style.display = 'flex';
    }

    // Update the generateVideoNotes function
    window.generateVideoNotes = function(videoUrl) {
        const videoContainer = document.querySelector('.video-container');
        const notesContainer = document.querySelector('.notes-container');
        const notesContent = document.querySelector('.notes-content');
        const generateBtn = document.querySelector('.generate-notes-btn');

        // Add the with-notes class to video container for smooth transition
        videoContainer.classList.add('with-notes');

        // Show notes container
        notesContainer.classList.add('show');

        // Disable the button and show the loading state
        generateBtn.disabled = true;
        generateBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Generating...';
        notesContent.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> Analyzing video and generating notes...</div>';

        // Send request to server to generate notes
        fetch('/WorkdrivePlusPlus/generateNotes', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ videoUrl: videoUrl })
        })
        .then(response => response.json())
        .then(data => {
            // Check if notes key exists in response
            if (data.notes) {
                notesContent.innerHTML = `<div class="notes-text">${data.notes}</div>`;
            } else {
                throw new Error("No notes returned from the server");
            }
            generateBtn.innerHTML = '<i class="fas fa-file-alt"></i> Regenerate Notes';
            generateBtn.disabled = false;
        })
        .catch(error => {
            notesContent.innerHTML = `<div class="error">
                <i class="fas fa-exclamation-circle"></i>
                Error generating notes: ${error.message || 'Unknown error'}
            </div>`;
            generateBtn.innerHTML = '<i class="fas fa-file-alt"></i> Retry';
            generateBtn.disabled = false;
        });
    };


    function getFileTypeIcon(type) {
        const iconMap = {
            'application/pdf': '<i class="fas fa-file-pdf"></i>',
            'application/msword': '<i class="fas fa-file-word"></i>',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document': '<i class="fas fa-file-word"></i>',
            'application/vnd.ms-excel': '<i class="fas fa-file-excel"></i>',
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': '<i class="fas fa-file-excel"></i>',
            'application/vnd.ms-powerpoint': '<i class="fas fa-file-powerpoint"></i>',
            'application/vnd.openxmlformats-officedocument.presentationml.presentation': '<i class="fas fa-file-powerpoint"></i>',
            'text/plain': '<i class="fas fa-file-alt"></i>',
            'audio/': '<i class="fas fa-file-audio"></i>',
            'application/json': '<i class="fas fa-file-code"></i>',
            'text/html': '<i class="fas fa-file-code"></i>',
            'text/css': '<i class="fas fa-file-code"></i>',
            'text/javascript': '<i class="fas fa-file-code"></i>',
        };

        // Check for exact matches
        if (iconMap[type]) {
            return iconMap[type];
        }

        // Check for partial matches (e.g., audio/*)
        for (const [key, icon] of Object.entries(iconMap)) {
            if (type.startsWith(key)) {
                return icon;
            }
        }

        // Default icon
        return '<i class="fas fa-file"></i>';
    }

    // Make functions globally available
    window.toggleMenu = function(button) {
        const menu = button.nextElementSibling;
        document.querySelectorAll('.action-menu').forEach(m => {
            if (m !== menu) m.classList.remove('show');
        });
        menu.classList.toggle('show');
    };

    window.toggleFavorite = function(element) {
        const fileCard = element.closest('.file-card');
        fileCard.classList.toggle('favorite');
        element.innerHTML = fileCard.classList.contains('favorite')
            ? '<i class="fas fa-star" style="color: #eab308;"></i>Remove from favorites'
            : '<i class="fas fa-star"></i>Add to favorites';
    };

    window.moveToTrash = function(element) {
        const fileCard = element.closest('.file-card');
        fileCard.classList.add('deleted');
        if (currentCategory !== 'trash') {
            fileCard.style.display = 'none';
        }
    };

    // Filter files based on category
    function filterFiles() {
        const fileCards = document.querySelectorAll('.file-card');
        fileCards.forEach(card => {
            const isDeleted = card.classList.contains('deleted');
            const isFavorite = card.classList.contains('favorite');
            const hasImage = card.querySelector('img.thumbnail') !== null;
            const hasVideo = card.querySelector('video.thumbnail') !== null;
            const hasPDF = card.querySelector('.fa-file-pdf') !== null;

            switch (currentCategory) {
                case 'home':
                    card.style.display = isDeleted ? 'none' : 'flex';
                    break;
                case 'favorites':
                    card.style.display = (isFavorite && !isDeleted) ? 'flex' : 'none';
                    break;
                case 'trash':
                    card.style.display = isDeleted ? 'flex' : 'none';
                    break;
                case 'images':
                    card.style.display = (hasImage && !isDeleted) ? 'flex' : 'none';
                    break;
                case 'videos':
                    card.style.display = (hasVideo && !isDeleted) ? 'flex' : 'none';
                    break;
                case 'pdf':
                    card.style.display = (hasPDF && !isDeleted) ? 'flex' : 'none';
                    break;
                default:
                    card.style.display = 'flex';
            }
        });
    }

    // Video notes generation
    window.generateVideoNotes = function(videoUrl) {
        const notesContainer = document.querySelector('.notes-container');
        const notesContent = document.querySelector('.notes-content');
        const generateBtn = document.querySelector('.generate-notes-btn');

        generateBtn.disabled = true;
        generateBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Generating...';
        notesContainer.style.display = 'block';
        notesContent.innerHTML = '<div class="loading">Generating notes...</div>';

        fetch('/WorkdrivePlusPlus/generate-notes', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ videoUrl: videoUrl })
        })
        .then(response => response.json())
        .then(data => {
            notesContent.innerHTML = `<div class="notes-text">${data.notes}</div>`;
            generateBtn.innerHTML = '<i class="fas fa-file-alt"></i> Regenerate Notes';
            generateBtn.disabled = false;
        })
        .catch(error => {
            notesContent.innerHTML = `<div class="error">Error generating notes: ${error.message}</div>`;
            generateBtn.innerHTML = '<i class="fas fa-file-alt"></i> Retry';
            generateBtn.disabled = false;
        });
    };

    window.copyNotes = function() {
        const notesText = document.querySelector('.notes-text').textContent;
        navigator.clipboard.writeText(notesText).then(() => {
            const copyBtn = document.querySelector('.copy-btn');
            copyBtn.innerHTML = '<i class="fas fa-check"></i> Copied!';
            setTimeout(() => {
                copyBtn.innerHTML = '<i class="fas fa-copy"></i> Copy';
            }, 2000);
        });
    };
});