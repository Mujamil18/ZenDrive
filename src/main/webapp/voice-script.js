 // Elements
    const searchInput = document.getElementById("searchInput");
    const searchButton = document.getElementById("searchButton");
    const voiceBtn = document.getElementById("voice-btn");
    const microphoneIcon = document.getElementById("voice-microphone-icon");
    const dropdown = document.getElementById("dropdown");

    // Initialize speech recognition
    const recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
    recognition.lang = 'en-US';
    recognition.interimResults = true;

    // Timer for debounce
    let debounceTimer;

    // Debounce function to delay search requests
    function debounce(func, delay) {
        return function () {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(func, delay);
        };
    }

    // Function to handle search query input and display dropdown
    function searchInputEvent() {
        const query = searchInput.value.trim();

        // Hide dropdown if the search input is empty
        if (query.length === 0) {
            dropdown.style.display = 'none';
            return;
        }

    // Start or stop voice recognition when microphone icon is clicked
    voiceBtn.addEventListener("click", function () {
        console.log("Voice button clicked...");

        if (microphoneIcon.classList.contains("listening")) {
            console.log("Stopping listening...");
            recognition.stop();
            microphoneIcon.classList.remove("listening");
        } else {
            console.log("Starting listening...");
            recognition.start();
            microphoneIcon.classList.add("listening");
        }
    });

    // Start event: Speech recognition has started
    recognition.onstart = function () {
        console.log("Speech recognition started...");
    };

    // Result event: Speech input detected and text is added to search box
    recognition.onresult = function (event) {
        const transcript = event.results[event.results.length - 1][0].transcript;
        console.log("Recognized Speech:", transcript);
        searchInput.value = transcript;

        // As you speak, initiate search request with debounce
        debounce(searchInputEvent, 500)(); // Delay by 500ms
    };

    // Error handling: If there's an error with speech recognition
    recognition.onerror = function (event) {
        console.error("Speech recognition error:", event.error);
        if (event.error === "not-allowed") {
            alert("Please allow microphone access in your browser settings.");
        }
    };

    // End event: When speech recognition stops
    recognition.onend = function () {
        console.log("Speech recognition ended.");
        const query = searchInput.value.trim();
        if (query) {
            searchButton.click();
        }
    };

    // Event listener to trigger search as you type or speak (debounced)
    searchInput.addEventListener("input", function () {
        debounce(searchInputEvent, 500)(); // Delay by 500ms
    });

    // Search button click event to initiate search manually
    searchButton.addEventListener("click", function() {
        const query = searchInput.value.trim();

        if (query) {
            console.log("Searching for:", query);
        } else {
            console.log("No search query provided");
        }
    });