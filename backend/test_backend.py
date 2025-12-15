"""
Simple test script for KK's Pokemon Alert Backend
Tests basic functionality without requiring Firebase credentials
"""

import sys
from main import check_website_status

def test_website_check():
    """Test the website checking functionality"""
    print("Testing website status check...")
    print("-" * 50)
    
    try:
        result = check_website_status()
        
        print(f"Status: {result.get('status')}")
        print(f"URL: {result.get('url')}")
        print(f"Timestamp: {result.get('timestamp')}")
        
        if 'keywords_found' in result:
            keywords = result['keywords_found']
            if keywords:
                print(f"Keywords found: {', '.join(keywords)}")
            else:
                print("No keywords found")
        
        if 'error' in result:
            print(f"Error: {result['error']}")
            return False
        
        print("\n✅ Website check completed successfully!")
        return True
        
    except Exception as e:
        print(f"\n❌ Test failed with error: {e}")
        return False

if __name__ == "__main__":
    success = test_website_check()
    sys.exit(0 if success else 1)
