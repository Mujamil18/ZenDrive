// Get elements
console.log("bot");
const botButton = document.querySelector('.bot');
const chatbot = document.getElementById('chatbot');
const closeBtn = document.getElementById('closeBtn');
const voiceButton = document.getElementById('voiceButton');

// Toggle chatbot visibility when bot is clicked
botButton.addEventListener('click', function() {
    console.log("hello event");
    if (chatbot.style.display === 'none' || chatbot.style.display === '') {
        chatbot.style.display = 'flex';
    } else {
        chatbot.style.display = 'none';
    }
});

// Close chatbot when close button is clicked
closeBtn.addEventListener('click', function() {
    chatbot.style.display = 'none';
});

// Chatbot sending/receiving functionality
const sendButton = document.getElementById("sendButton");
const inputText = document.getElementById("inputText");
const chatBox = document.getElementById("chatBox");

const profiles = {
    sent: 'https://photoshulk.com/wp-content/uploads/best-meme-pfp-for-school.jpg',
    received: 'https://preview.redd.it/bcyq3rjk2w071.png?auto=webp&s=97c9b873f1b41a7b9ff31331fd92f2e3fafed92f'
};

// Send Button functionality
sendButton.addEventListener("click", function() {
    const message = inputText.value.trim();
    if (message) {
        const messageElement = document.createElement("div");
        messageElement.classList.add("message", "sent");

        const profileImg = document.createElement("img");
        profileImg.src = profiles.sent;
        profileImg.classList.add("profile-img");

        const textElement = document.createElement("span");
        textElement.textContent = message;

        messageElement.appendChild(textElement);
        messageElement.appendChild(profileImg);
        chatBox.appendChild(messageElement);
        chatBox.scrollTop = chatBox.scrollHeight;
        inputText.value = "";

        fetch('/WorkdrivePlusPlus/bot', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ message: message })
        })
        .then(response => response.text())
        .then(data => {
            const replyMessage = data;
            const replyElement = document.createElement("div");
            replyElement.classList.add("message", "received");

            const replyProfileImg = document.createElement("img");
            replyProfileImg.src = profiles.received;
            replyProfileImg.classList.add("profile-img");

            const replyTextElement = document.createElement("span");
            replyTextElement.textContent = replyMessage;

            replyElement.appendChild(replyProfileImg);
            replyElement.appendChild(replyTextElement);
            chatBox.appendChild(replyElement);
            chatBox.scrollTop = chatBox.scrollHeight;

            // Text-to-Speech - Convert reply to speech
            const speech = new SpeechSynthesisUtterance(replyMessage);
            window.speechSynthesis.speak(speech);
        })
        .catch(error => {
            console.error('Error:', error);
            const errorMessage = document.createElement("div");
            errorMessage.classList.add("message", "received");
            errorMessage.textContent = "Sorry, there was an error processing your request.";
            chatBox.appendChild(errorMessage);
            chatBox.scrollTop = chatBox.scrollHeight;
        });
    }
});

// Triggering send on Enter key press
inputText.addEventListener("keydown", function(event) {
    if (event.key === "Enter") {
        sendButton.click();
    }
});

// Speech-to-Text functionality (for the voice button)
const recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
recognition.lang = 'en-US';
recognition.continuous = false; // Stop after one result
recognition.interimResults = false; // Don't show partial results

voiceButton.addEventListener('click', function() {
    recognition.start(); // Start listening to the user's speech
    console.log("Listening for speech...");
});

// Handle speech recognition results
recognition.onresult = function(event) {
    const transcript = event.results[0][0].transcript;
    console.log("Recognized text:", transcript);
    inputText.value = transcript; // Populate the text input with recognized speech
    sendButton.click(); // Send the message automatically after speech-to-text conversion
};

// Handle speech recognition errors
recognition.onerror = function(event) {
    console.error('Speech recognition error:', event.error);
};

// Handle speech recognition end (optional, in case user stops speaking)
recognition.onend = function() {
    console.log('Speech recognition ended.');
};
