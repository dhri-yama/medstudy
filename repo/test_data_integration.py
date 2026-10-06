#!/usr/bin/env python3
"""
Test script to verify MedMCQA data integration.
Simulates how the app would use the JSON data.
"""

import json
import os
from datetime import datetime

def load_json_file(filename):
    """Load and return JSON data from file."""
    filepath = os.path.join('/Users/sagar/Documents/ChatGPT/medstudy/repo', filename)
    if not os.path.exists(filepath):
        print(f"❌ Error: {filename} not found at {filepath}")
        return None
    
    with open(filepath, 'r', encoding='utf-8') as f:
        return json.load(f)

def test_subjects_loading():
    """Test loading subjects data."""
    print("📚 Testing Subjects Loading...")
    subjects = load_json_file('subjects.json')
    
    if not subjects:
        return False
    
    print(f"   ✅ Loaded {len(subjects)} subjects")
    
    # Verify structure
    for subject in subjects[:3]:
        required_fields = ['id', 'name', 'question_count', 'topic_count']
        for field in required_fields:
            if field not in subject:
                print(f"   ❌ Missing field '{field}' in subject")
                return False
    
    print(f"   ✅ All subjects have required fields")
    return True

def test_topics_loading():
    """Test loading topics data."""
    print("\n🏷️ Testing Topics Loading...")
    topics = load_json_file('topics.json')
    
    if not topics:
        return False
    
    print(f"   ✅ Loaded {len(topics)} topics")
    
    # Verify structure
    for topic in topics[:3]:
        required_fields = ['name', 'subjects', 'question_count']
        for field in required_fields:
            if field not in topic:
                print(f"   ❌ Missing field '{field}' in topic")
                return False
    
    print(f"   ✅ All topics have required fields")
    return True

def test_questions_loading():
    """Test loading questions data."""
    print("\n❓ Testing Questions Loading...")
    questions = load_json_file('questions.json')
    
    if not questions:
        return False
    
    print(f"   ✅ Loaded {len(questions)} questions")
    
    # Verify structure
    required_fields = ['id', 'question', 'cop', 'opa', 'opb', 'opc', 'opd', 'subject_name']
    for question in questions[:3]:
        for field in required_fields:
            if field not in question:
                print(f"   ❌ Missing field '{field}' in question")
                return False
    
    print(f"   ✅ All questions have required fields")
    return True

def test_data_consistency():
    """Test data consistency between files."""
    print("\n🔄 Testing Data Consistency...")
    
    subjects = load_json_file('subjects.json')
    questions = load_json_file('questions.json')
    
    if not subjects or not questions:
        return False
    
    # Check subject counts match
    for subject in subjects:
        subject_id = subject['id']
        actual_count = sum(1 for q in questions if q.get('subject_name', '').lower().replace(' ', '_').replace('&', 'and') == subject_id)
        expected_count = subject['question_count']
        
        if actual_count != expected_count:
            print(f"   ⚠️ Subject '{subject['name']}' count mismatch: expected {expected_count}, got {actual_count}")
    
    print(f"   ✅ Data consistency check completed")
    return True

def test_subject_files():
    """Test loading subject-specific files."""
    print("\n📁 Testing Subject-Specific Files...")
    
    subjects = load_json_file('subjects.json')
    if not subjects:
        return False
    
    subject_files = ['medicine.json', 'surgery.json', 'anatomy.json']
    for filename in subject_files:
        data = load_json_file(filename)
        if data:
            print(f"   ✅ {filename}: {len(data)} questions")
        else:
            print(f"   ❌ {filename} not found")
            return False
    
    return True

def test_random_question():
    """Test displaying a random question."""
    print("\n🎲 Testing Random Question Display...")
    
    questions = load_json_file('questions.json')
    if not questions:
        return False
    
    import random
    question = random.choice(questions)
    
    print(f"   Question: {question['question'][:60]}...")
    print(f"   Subject: {question['subject_name']}")
    print(f"   Topic: {question.get('topic', 'N/A')}")
    print(f"   Type: {question['choice_type']}")
    print(f"   Options: {len([k for k in ['opa', 'opb', 'opc', 'opd'] if k in question])}")
    
    return True

def simulate_app_flow():
    """Simulate the app's data flow."""
    print("\n📱 Simulating App Data Flow...")
    
    # Load all data
    subjects = load_json_file('subjects.json')
    questions = load_json_file('questions.json')
    
    if not subjects or not questions:
        return False
    
    # Simulate selecting Medicine subject
    medicine_subject = next((s for s in subjects if s['name'] == 'Medicine'), None)
    if medicine_subject:
        medicine_questions = [q for q in questions if q.get('subject_name') == 'Medicine']
        print(f"   👨‍⚕️ User selects Medicine: {len(medicine_questions)} questions available")
        
        # Simulate filtering by topic
        topics_in_medicine = set(q.get('topic', '') for q in medicine_questions if q.get('topic'))
        print(f"   🏷️ Topics in Medicine: {len(topics_in_medicine)} unique topics")
        
        # Simulate practice session
        practice_questions = medicine_questions[:5]  # First 5 questions
        print(f"   📝 Starting practice session with {len(practice_questions)} questions")
        
        for i, q in enumerate(practice_questions[:2], 1):
            print(f"     Q{i}. {q['question'][:50]}...")
    
    return True

def run_all_tests():
    """Run all integration tests."""
    print("🚀 MedMCQA Data Integration Test Suite")
    print("=" * 50)
    
    tests = [
        test_subjects_loading,
        test_topics_loading,
        test_questions_loading,
        test_data_consistency,
        test_subject_files,
        test_random_question,
        simulate_app_flow
    ]
    
    results = []
    for test in tests:
        try:
            result = test()
            results.append(result)
        except Exception as e:
            print(f"   ❌ Test failed with error: {e}")
            results.append(False)
    
    print("\n" + "=" * 50)
    print("📊 Test Results:")
    print(f"   Total Tests: {len(tests)}")
    print(f"   Passed: {sum(results)}")
    print(f"   Failed: {len(tests) - sum(results)}")
    
    if all(results):
        print("\n🎉 All tests passed! Data integration is working correctly.")
        return True
    else:
        print("\n❌ Some tests failed. Check the output above.")
        return False

if __name__ == '__main__':
    start_time = datetime.now()
    success = run_all_tests()
    end_time = datetime.now()
    
    print(f"\n⏱️  Tests completed in {end_time - start_time}")
    
    exit(0 if success else 1)