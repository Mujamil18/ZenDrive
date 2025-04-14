// Select the modal and close button
var modal = document.getElementById("imageModal");
var closeModal = document.getElementById("closeModal");

// Function to open modal and show clicked image
function openModal(imageSrc) {
    var modalImage = document.getElementById("modalImage");
    modalImage.src = imageSrc;  // Set the src of the image to be shown in the modal
    modal.style.display = "block";  // Display the modal
}

// When the user clicks on the close button, close the modal
closeModal.onclick = function () {
    modal.style.display = "none";
}

// When the user clicks anywhere outside the modal, close it
window.onclick = function (event) {
    if (event.target == modal) {
        modal.style.display = "none";
    }
}
