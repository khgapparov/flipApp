import React from 'react';
import { MessageCircle, Send, User, MessageSquare, LogIn } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

const ChatView = ({ chatMessages, newMessage, setNewMessage, sendMessage, chatEndRef, formatDate }) => {
  const { user: authUser, loading: authLoading } = useAuth();
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-bold text-gray-900">Project Chat</h2>
        <div className="flex items-center text-gray-600">
          <MessageCircle size={18} className="mr-2" />
          <span className="text-sm">{chatMessages.length} messages</span>
        </div>
      </div>

      {/* Chat Container */}
      <div className="card p-0 overflow-hidden">
        {/* Chat Header */}
        <div className="bg-blue-600 px-6 py-4">
          <div className="flex items-center">
            <div className="bg-white p-2 rounded-full">
              <MessageSquare size={20} className="text-blue-600" />
            </div>
            <div className="ml-3">
              <h3 className="text-white font-semibold">Project Communication</h3>
              <p className="text-blue-100 text-sm">Chat with your project team</p>
            </div>
          </div>
        </div>

        {/* Chat Messages */}
        <div className="h-96 overflow-y-auto p-6 bg-gray-50">
          {chatMessages.length === 0 ? (
            <div className="text-center py-12">
              <div className="bg-blue-100 p-6 rounded-full inline-block mb-6">
                <MessageCircle size={40} className="text-blue-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-3">Start a Conversation</h3>
              {!authUser ? (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4 max-w-md mx-auto">
                  <p className="text-blue-800 text-sm">
                    Please log in to start chatting with your project team
                  </p>
                </div>
              ) : authUser.isAnonymous ? (
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4 max-w-md mx-auto">
                  <p className="text-yellow-800 text-sm">
                    You're using a guest account. Chat messages will be saved temporarily.
                  </p>
                </div>
              ) : (
                <p className="text-gray-600 max-w-md mx-auto">
                  Send a message to your project manager to discuss your project progress and any questions you may have.
                </p>
              )}
            </div>
          ) : (
            <div className="space-y-4">
              {chatMessages.map((message) => (
                <div
                  key={message.id}
                  className={`flex ${message.sender === authUser?.id ? 'justify-end' : 'justify-start'}`}
                >
                  <div className={`max-w-xs lg:max-w-md p-4 rounded-2xl ${
                    message.sender === authUser?.id
                      ? 'bg-blue-600 text-white'
                      : 'bg-white text-gray-800 shadow-sm border border-gray-200'
                  }`}>
                    <div className="flex items-center mb-2">
                      <div className={`p-1 rounded-full ${
                        message.sender === authUser?.id
                          ? 'bg-blue-500'
                          : 'bg-gray-200'
                      }`}>
                        <User size={12} className={
                          message.sender === authUser?.id
                            ? 'text-white'
                            : 'text-gray-600'
                        } />
                      </div>
                      <span className={`text-xs ml-2 ${
                        message.sender === authUser?.id
                          ? 'text-blue-100'
                          : 'text-gray-500'
                      }`}>
                        {message.senderName}
                      </span>
                    </div>
                    
                    <p className="text-sm leading-relaxed">{message.text}</p>
                    
                    <p className={`text-xs mt-2 ${
                      message.sender === authUser?.id
                        ? 'text-blue-200'
                        : 'text-gray-400'
                    }`}>
                      {formatDate(message.createdAt)}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
          <div ref={chatEndRef} />
        </div>

        {/* Message Input */}
        <div className="border-t border-gray-200 p-4 bg-white">
          {!authUser ? (
            <div className="text-center py-4">
              <div className="bg-blue-100 p-3 rounded-lg mb-3">
                <LogIn size={20} className="text-blue-600 mx-auto mb-2" />
                <p className="text-blue-800 text-sm">
                  Please log in to participate in the chat
                </p>
              </div>
            </div>
          ) : authUser.isAnonymous ? (
            <div className="text-center py-4">
              <div className="bg-yellow-100 p-3 rounded-lg mb-3">
                <p className="text-yellow-800 text-sm">
                  You're using a guest account. Chat messages will be saved temporarily.
                </p>
              </div>
              <div className="flex space-x-3">
                <input
                  type="text"
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
                  placeholder="Type your message..."
                  className="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
                />
                <button
                  onClick={sendMessage}
                  disabled={!newMessage.trim()}
                  className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors flex items-center"
                >
                  <Send size={18} className="mr-2" />
                  Send
                </button>
              </div>
            </div>
          ) : (
            <div className="flex space-x-3">
              <input
                type="text"
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
                placeholder="Type your message..."
                className="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
              />
              <button
                onClick={sendMessage}
                disabled={!newMessage.trim()}
                className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors flex items-center"
              >
                <Send size={18} className="mr-2" />
                Send
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Chat Tips */}
      <div className="card bg-blue-50 border-blue-200">
        <div className="flex items-start">
          <MessageCircle size={20} className="text-blue-600 mr-3 mt-0.5" />
          <div>
            <h3 className="text-sm font-medium text-blue-900 mb-1">Chat Tips</h3>
            <p className="text-sm text-blue-700">
              Use this chat to discuss project progress, ask questions, and coordinate with your project team.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChatView;
