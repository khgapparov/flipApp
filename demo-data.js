// Demo data for testing the Lovable Cline application
// This file can be used to populate Firestore with sample data

const demoProject = {
    name: "123 Main Street Flip",
    address: "123 Main Street, Anytown, CA 12345",
    status: "Renovation",
    startDate: new Date("2025-01-15"),
    estimatedEndDate: new Date("2025-06-30"),
    projectedProfit: "$75,000 - $100,000"
};

const demoUpdates = [
    {
        description: "Demolition completed. All interior walls and fixtures removed.",
        timestamp: new Date("2025-01-20")
    },
    {
        description: "Electrical rough-in completed. All wiring installed to code.",
        timestamp: new Date("2025-02-05")
    },
    {
        description: "Plumbing rough-in completed. New pipes installed throughout.",
        timestamp: new Date("2025-02-18")
    },
    {
        description: "HVAC system installed. New ductwork and units operational.",
        timestamp: new Date("2025-03-02")
    },
    {
        description: "Drywall installation completed. Ready for finishing.",
        timestamp: new Date("2025-03-15")
    }
];

const demoGallery = [
    {
        url: "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=400&h=300&fit=crop",
        caption: "Before - Living Room",
        timestamp: new Date("2025-01-10")
    },
    {
        url: "https://images.unsplash.com/photo-1560448204-603b3fc33ddc?w=400&h=300&fit=crop",
        caption: "After Demolition",
        timestamp: new Date("2025-01-21")
    },
    {
        url: "https://images.unsplash.com/photo-1560448205-17d3a46c84de?w=400&h=300&fit=crop",
        caption: "Electrical Work",
        timestamp: new Date("2025-02-06")
    },
    {
        url: "https://images.unsplash.com/photo-1560448205-4d9b3e6bb6db?w=400&h=300&fit=crop",
        caption: "Plumbing Installation",
        timestamp: new Date("2025-02-19")
    },
    {
        url: "https://images.unsplash.com/photo-1560448205-4d9b3e6bb6db?w=400&h=300&fit=crop",
        caption: "Drywall Complete",
        timestamp: new Date("2025-03-16")
    }
];

const demoChatMessages = [
    {
        text: "Welcome to your project portal! Feel free to ask any questions about the renovation progress.",
        sender: "manager",
        senderName: "Project Manager",
        timestamp: new Date("2025-01-16")
    },
    {
        text: "When can we expect to see the kitchen cabinets installed?",
        sender: "client",
        senderName: "Client",
        timestamp: new Date("2025-02-10")
    },
    {
        text: "Kitchen cabinets are scheduled for installation next week. We'll send photos once they're in!",
        sender: "manager",
        senderName: "Project Manager",
        timestamp: new Date("2025-02-10")
    }
];

// Function to populate Firestore (for demo purposes)
async function populateDemoData(firestore, appId, projectId = 'demo-project-123') {
    try {
        // Create project document
        const projectRef = firestore.doc(`/artifacts/${appId}/public/data/projects/${projectId}`);
        await projectRef.set({
            ...demoProject,
            startDate: firestore.FieldValue.serverTimestamp(),
            estimatedEndDate: firestore.FieldValue.serverTimestamp()
        });

        // Add updates
        const updatesRef = projectRef.collection('updates');
        for (const update of demoUpdates) {
            await updatesRef.add({
                ...update,
                timestamp: firestore.FieldValue.serverTimestamp()
            });
        }

        // Add gallery images
        const galleryRef = projectRef.collection('gallery');
        for (const image of demoGallery) {
            await galleryRef.add({
                ...image,
                timestamp: firestore.FieldValue.serverTimestamp()
            });
        }

        // Add chat messages
        const chatRef = projectRef.collection('chat');
        for (const message of demoChatMessages) {
            await chatRef.add({
                ...message,
                timestamp: firestore.FieldValue.serverTimestamp()
            });
        }

        console.log('Demo data populated successfully!');
    } catch (error) {
        console.error('Error populating demo data:', error);
    }
}

// Export for use in browser console
window.populateDemoData = populateDemoData;
window.demoData = { demoProject, demoUpdates, demoGallery, demoChatMessages };
