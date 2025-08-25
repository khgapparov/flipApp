// Script to create demo project with correct date format
const API_BASE_URL = 'http://localhost:8080';

async function createDemoProject() {
  try {
    // First, get an anonymous login token
    console.log('Getting anonymous login token...');
    const loginResponse = await fetch(`${API_BASE_URL}/api/auth/anonymous`, {
      method: 'POST'
    });
    
    if (!loginResponse.ok) {
      throw new Error(`Login failed: ${loginResponse.status}`);
    }
    
    const loginData = await loginResponse.json();
    const token = loginData.access_token;
    const userId = loginData.user_id;
    
    console.log('Got token:', token);
    console.log('User ID:', userId);
    
    // First, try to access the existing demo project
    console.log('Checking if demo project exists...');
    try {
      const testResponse = await fetch(`${API_BASE_URL}/api/projects/demo-project-123`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (testResponse.ok) {
        const testProject = await testResponse.json();
        console.log('Demo project already exists and is accessible:', testProject);
        return;
      }
    } catch (accessError) {
      console.log('Demo project not accessible with current user, will create a new one...');
    }
    
    // Create the demo project with correct date format
    const projectData = {
      id: 'demo-project-123', // Set the specific ID that's causing 404
      name: "123 Main Street Flip",
      address: "123 Main Street, Anytown, CA 12345",
      status: "Renovation",
      startDate: "2025-01-15T00:00:00", // ISO format with time
      estimatedEndDate: "2025-06-30T00:00:00", // ISO format with time
      projectedProfitStatus: "on track",
      ownerId: userId
    };
    
    console.log('Creating demo project...');
    const createResponse = await fetch(`${API_BASE_URL}/api/projects`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(projectData)
    });
    
    if (!createResponse.ok) {
      const errorText = await createResponse.text();
      console.error('Create project failed:', createResponse.status, errorText);
      
        // If project already exists but for different user, try to transfer ownership
        if (createResponse.status === 400 && errorText.includes('already exists')) {
          console.log('Project exists but for different user, trying to transfer ownership...');
          
          const transferResponse = await fetch(`${API_BASE_URL}/api/projects/demo/demo-project-123/transfer`, {
            method: 'POST',
            headers: {
              'Authorization': `Bearer ${token}`
            }
          });
          
          if (!transferResponse.ok) {
            throw new Error(`Transfer failed: ${transferResponse.status}`);
          }
          
          const transferredProject = await transferResponse.json();
          console.log('Demo project ownership transferred successfully:', transferredProject);
        } else {
          throw new Error(`Create project failed: ${createResponse.status}`);
        }
    } else {
      const createdProject = await createResponse.json();
      console.log('Demo project created successfully:', createdProject);
    }
    
    // Test that we can now access the project
    console.log('Testing project access...');
    const testResponse = await fetch(`${API_BASE_URL}/api/projects/demo-project-123`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!testResponse.ok) {
      throw new Error(`Test access failed: ${testResponse.status}`);
    }
    
    const testProject = await testResponse.json();
    console.log('Project access successful:', testProject);
    
  } catch (error) {
    console.error('Error creating demo project:', error);
  }
}

// Run the script
createDemoProject();
