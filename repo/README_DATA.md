# MedMCQA Data Organization

This directory contains organized JSON files extracted from the MedMCQA dataset for easy integration into medical education applications.

## 📊 Dataset Overview

- **Total Questions**: 1,618 MCQs
- **NEET PG Questions**: 857 questions
- **AIIMS Questions**: 761 questions
- **Medical Subjects**: 21 subjects
- **Medical Topics**: 141 unique topics
- **Question Types**: Single-choice and Multi-choice

## 📁 File Structure

### Core Metadata Files

| File | Description | Usage |
|------|-------------|-------|
| `subjects.json` | List of all medical subjects with statistics | Display subject list, navigation |
| `topics.json` | List of all medical topics with related subjects | Topic-based filtering, exploration |
| `exam_types.json` | Information about exam types (NEET PG, AIIMS) | Exam selection, metadata |
| `stats.json` | Comprehensive statistics about the dataset | Analytics, insights |
| `questions.json` | All questions combined with metadata | Full dataset access |

### Subject-Specific Files

Each medical subject has its own JSON file containing all questions for that subject:

- `anaesthesia.json` (27 questions)
- `anatomy.json` (84 questions)
- `biochemistry.json` (88 questions)
- `dental.json` (22 questions)
- `ent.json` (44 questions)
- `forensic_medicine.json` (398 questions)
- `gynaecology_and_obstetrics.json` (102 questions)
- `medicine.json` (140 questions)
- `microbiology.json` (68 questions)
- `ophthalmology.json` (48 questions)
- `orthopaedics.json` (4 questions)
- `pathology.json` (116 questions)
- `pediatrics.json` (72 questions)
- `pharmacology.json` (126 questions)
- `physiology.json` (66 questions)
- `psychiatry.json` (64 questions)
- `radiology.json` (60 questions)
- `skin.json` (16 questions)
- `social_and_preventive_medicine.json` (50 questions)
- `surgery.json` (117 questions)

## 📖 Data Structure

### Question Object Structure

```json
{
  "id": "unique-identifier-uuid",
  "question": "The question text",
  "exp": "Expert explanation (may be null)",
  "cop": 1,  // Correct option index (1=a, 2=b, 3=c, 4=d)
  "opa": "Option A text",
  "opb": "Option B text", 
  "opc": "Option C text",
  "opd": "Option D text",
  "subject_name": "Medicine",
  "topic": "Cardiovascular System",
  "choice_type": "single",  // or "multi"
  "source": "NEET PG"  // or "AIIMS"
}
```

### Subject Object Structure (subjects.json)

```json
{
  "id": "medicine",
  "name": "Medicine",
  "question_count": 140,
  "topic_count": 25,
  "exam_types": ["NEET PG", "AIIMS"]
}
```

### Topic Object Structure (topics.json)

```json
{
  "name": "Cardiovascular System",
  "subjects": ["Medicine", "Physiology"],
  "question_count": 45
}
```

## 🚀 Usage Examples

### JavaScript/TypeScript

```javascript
// Load all questions
import questions from './questions.json';

// Load by subject
import medicineQuestions from './medicine.json';

// Load metadata
import subjects from './subjects.json';
import topics from './topics.json';

// Example: Get all Medicine questions
const medicineQs = questions.filter(q => q.subject_name === 'Medicine');

// Example: Get questions for a specific topic
const cvsQuestions = questions.filter(q => q.topic === 'C.V.S.');
```

### Python

```python
import json

# Load all questions
with open('questions.json') as f:
    questions = json.load(f)

# Load subjects
with open('subjects.json') as f:
    subjects = json.load(f)

# Get Medicine questions
medicine_questions = [q for q in questions if q['subject_name'] == 'Medicine']

# Get statistics
with open('stats.json') as f:
    stats = json.load(f)

print(f"Total questions: {stats['total_questions']}")
```

### React Native / Mobile App

```javascript
// Import for subject list
const subjects = require('./subjects.json');

// Display subject list
const SubjectList = () => (
  <FlatList
    data={subjects}
    keyExtractor={(item) => item.id}
    renderItem={({item}) => (
      <SubjectItem 
        name={item.name} 
        count={item.question_count} 
        onPress={() => navigation.navigate('Subject', {subject: item})}
      />
    )}
  />
);

// Load questions for a subject
const SubjectQuestions = ({route}) => {
  const {subject} = route.params;
  const questions = require(`./${subject.id}.json`);
  
  return <QuestionList questions={questions} />;
};
```

## 🎯 Common Use Cases

### 1. Subject-Based Navigation
- Use `subjects.json` to create a subject list
- Each subject links to its specific JSON file
- Show question counts for each subject

### 2. Topic-Based Learning
- Use `topics.json` for topic exploration
- Filter questions by topic across subjects
- Create topic-based study sessions

### 3. Exam-Specific Preparation
- Use `source` field to filter by exam type
- Create NEET PG and AIIMS specific modules
- Track progress by exam type

### 4. Mixed Practice Sessions
- Use `questions.json` for random question selection
- Filter by difficulty, subject, or topic
- Create custom practice sets

## 📈 Statistics (from stats.json)

```json
{
  "total_questions": 1618,
  "neet_pg_questions": 857,
  "aiims_questions": 761,
  "subject_distribution": {
    "Forensic Medicine": 398,
    "Medicine": 140,
    "Pharmacology": 126,
    "Surgery": 117,
    "Pathology": 116,
    "Gynaecology & Obstetrics": 102,
    "Microbiology": 68,
    "Ophthalmology": 48,
    "ENT": 44,
    "Anatomy": 84
  },
  "choice_type_distribution": {
    "single": 1024,
    "multi": 594
  }
}
```

## 🛠️ Data Management

### Updating Data
1. Add new JSON files to this directory
2. Update `subjects.json` if new subjects are added
3. Update `topics.json` if new topics are added
4. Regenerate `stats.json` and `questions.json` as needed

### File Naming Conventions
- Subject files: `subject_name.json` (lowercase, spaces replaced with underscores)
- Topic files: Not included by default (too many individual files)
- Metadata files: Use plural names (subjects, topics, exam_types, stats)

## 🎓 Medical Subjects Covered

1. Anaesthesia
2. Anatomy
3. Biochemistry
4. Dental
5. ENT (Ear, Nose, Throat)
6. Forensic Medicine
7. Gynaecology & Obstetrics
8. Medicine
9. Microbiology
10. Ophthalmology
11. Orthopaedics
12. Pathology
13. Pediatrics
14. Pharmacology
15. Physiology
16. Psychiatry
17. Radiology
18. Skin
19. Social & Preventive Medicine
20. Surgery

## 📚 Data Sources

- **NEET PG**: National Eligibility cum Entrance Test - Post Graduate
- **AIIMS**: All India Institute of Medical Sciences Entrance Exam
- **Original Dataset**: MedMCQA (21 medical subjects, 194k+ questions)
- **Extraction**: Filtered for NEET PG and AIIMS questions from 2020 onwards

## 🔒 License

The original MedMCQA dataset is under MIT License. This organized data maintains the same licensing terms.