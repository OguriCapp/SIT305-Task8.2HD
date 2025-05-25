#!/usr/bin/env python3
import requests
from bs4 import BeautifulSoup
import json
import os
import re
import time
from urllib.parse import urljoin

# URLs to scrape
URLS = {
    # News and Media
    'news': 'https://www.deakin.edu.au/about-deakin/news-and-media-releases',
    
    # Courses and Study
    'courses': 'https://www.deakin.edu.au/courses',
    'future_students': 'https://www.deakin.edu.au/study-at-deakin',
    
    # Careers and Employment
    'careers': 'https://www.deakin.edu.au/students/jobs-career',
    
    # International Students
    'international': 'https://www.deakin.edu.au/international-students',
    'international_living': 'https://www.deakin.edu.au/international-students/living-in-australia',
    
    # Research and Innovation
    'research': 'https://www.deakin.edu.au/research',
    'research_impact': 'https://www.deakin.edu.au/research/impact',
    'research_students': 'https://www.deakin.edu.au/research/become-a-research-student',
    
    # Student Life
    'student_life': 'https://www.deakin.edu.au/life-at-deakin/student-life',
    'clubs': 'https://www.deakin.edu.au/life-at-deakin/student-life/clubs-and-societies',
    
    # Campus Facilities
    'campuses': 'https://www.deakin.edu.au/about-deakin/locations/campuses',
    'library': 'https://www.deakin.edu.au/library',
    'accommodation': 'https://www.deakin.edu.au/life-at-deakin/accommodation',
    
    # Academic Support
    'study_support': 'https://www.deakin.edu.au/students/studying/study-support',
    'academic_skills': 'https://www.deakin.edu.au/students/studying/study-support/academic-skills',
    'library_help': 'https://www.deakin.edu.au/library/help',
    
    # Online Systems
    'cloud_deakin': 'https://www.deakin.edu.au/students/help/about-clouddeakin',
    'ontrack': 'https://www.deakin.edu.au/students/help/about-clouddeakin/assessment/ontrack',
    'student_connect': 'https://www.deakin.edu.au/students/enrolment-fees-and-money'
}

# System descriptions directly from Deakin University website
SYSTEM_DESCRIPTIONS = {
    'OnTrack': 'OnTrack is an online assessment tool that provides students with a task-oriented approach to portfolio assessment. Students work through a series of tasks in order to complete their unit learning outcomes.',
    'CloudDeakin': 'Understanding the Minibar, CloudDeakin Home page & setting your preferences. Our useful tools can help you stay organised, keep your files stored safely in the learning environment, and help you navigate your CloudDeakin unit content.',
    'DeakinSync': 'DeakinSync is your personalised student portal. Log in to access your units, manage your enrolment, collaborate with peers, view your timetable, and much more.',
    'StudentConnect': 'StudentConnect helps you to manage your enrolment, from enrolling and updating your course and units, your personal details, finding out your exam timetable and results to applying to graduate.',
    'DeakinTALENT': 'DeakinTALENT is Deakin\'s employment and careers service, available to you for the rest of your working life. They\'re experts at connecting you to your future career, and can help you research different career options, hone your interview skills and find casual work while you study.'
}

# Headers to mimic a browser
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.5',
    'DNT': '1',
    'Connection': 'keep-alive',
    'Upgrade-Insecure-Requests': '1',
}

def fetch_url(url):
    """Get the HTML content from a URL"""
    try:
        print(f"Scraping: {url}")
        response = requests.get(url, headers=HEADERS, timeout=10)
        response.raise_for_status()
        return response.text
    except requests.exceptions.RequestException as e:
        print(f"Error fetching {url}: {e}")
        return None

def clean_text(text):
    """Clean and normalize text"""
    if not text:
        return ""
    # Remove extra whitespace
    text = re.sub(r'\s+', ' ', text).strip()
    # Remove non-printable characters
    text = re.sub(r'[\x00-\x1F\x7F-\x9F]', '', text)
    return text

def extract_campus_info(html):
    """Extract information about Deakin campuses"""
    soup = BeautifulSoup(html, 'html.parser')
    campuses = []
    
    # Find main content
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        main_content = soup
    
    # Try multiple approaches to find campus information
    # 1. Look for campus sections
    campus_sections = main_content.select('.section, .grid-row, .card, [data-campus], .campus-info')
    for section in campus_sections:
        heading = section.select_one('h2, h3, h4, .title, .heading')
        if heading:
            campus_name = clean_text(heading.get_text())
            description = []
            
            # Get paragraphs
            paragraphs = section.select('p')
            for p in paragraphs:
                text = clean_text(p.get_text())
                if text and len(text) > 20:
                    description.append(text)
            
            if description:
                campuses.append({
                    'name': campus_name,
                    'description': ' '.join(description)
                })
    
    # 2. Look for headings that might indicate campus sections
    if not campuses:
        headings = main_content.select('h2, h3, h4')
        for heading in headings:
            text = heading.get_text().lower()
            if any(campus in text for campus in ['campus', 'melbourne', 'geelong', 'waurn ponds', 'waterfront', 'warrnambool']):
                campus_name = clean_text(heading.get_text())
                description = []
                
                # Get the next few paragraphs after the heading
                element = heading.next_sibling
                while element and len(description) < 5:  # Increased from 3 to 5
                    if hasattr(element, 'name') and element.name == 'p':
                        text = clean_text(element.get_text())
                        if text and len(text) > 20:
                            description.append(text)
                    element = element.next_sibling
                
                if description:
                    campuses.append({
                        'name': campus_name,
                        'description': ' '.join(description)
                    })
    
    # 3. Look for any content blocks that might contain campus information
    if not campuses:
        content_blocks = main_content.select('.content-block, .info-block, article')
        for block in content_blocks:
            block_text = block.get_text().lower()
            if any(campus in block_text for campus in ['campus', 'melbourne', 'geelong', 'waurn ponds', 'waterfront', 'warrnambool']):
                heading = block.select_one('h2, h3, h4, .title')
                if heading:
                    campus_name = clean_text(heading.get_text())
                    description = []
                    
                    paragraphs = block.select('p')
                    for p in paragraphs:
                        text = clean_text(p.get_text())
                        if text and len(text) > 20:
                            description.append(text)
                    
                    if description:
                        campuses.append({
                            'name': campus_name,
                            'description': ' '.join(description)
                        })
    
    return campuses

def extract_specific_campus_info(html, campus_name):
    """Extract detailed information about a specific campus"""
    soup = BeautifulSoup(html, 'html.parser')
    description = []
    features = []
    
    # Find main content area
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        main_content = soup  # Use the entire document if main content can't be found
    
    # Extract campus description from first substantial paragraphs
    paragraphs = main_content.select('p')
    for p in paragraphs:
        text = clean_text(p.get_text())
        if text and len(text) > 40:  # Skip very short texts
            description.append(text)
        if len(description) >= 3:  # Get up to 3 substantial paragraphs
            break
    
    # Extract features from lists, cards, or highlighted sections
    feature_sections = main_content.select('.feature, .facility, .highlight, .card, .grid-item')
    if feature_sections:
        for section in feature_sections:
            heading = section.select_one('h3, h4, h5, .title, .heading')
            if heading:
                feature_text = clean_text(heading.get_text())
                if feature_text:
                    features.append(feature_text)
    
    # If no specific features found, try to extract from list items
    if not features:
        list_items = main_content.select('ul li, ol li')
        for item in list_items:
            feature_text = clean_text(item.get_text())
            if feature_text and len(feature_text) > 10:
                features.append(feature_text)
    
    return {
        'name': campus_name,
        'description': ' '.join(description[:3]),  # Take first 3 paragraphs for conciseness
        'features': features[:5]  # Take up to 5 features
    }

def extract_faculty_info(html):
    """Extract information about faculties and schools"""
    soup = BeautifulSoup(html, 'html.parser')
    faculties = []
    
    # Find main content
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        main_content = soup
    
    # Look for faculty headings and sections in the new Deakin website structure
    faculty_headings = main_content.select('h2, h3')
    
    for heading in faculty_headings:
        heading_text = clean_text(heading.get_text())
        
        # Skip if not a faculty heading
        if not any(keyword in heading_text.lower() for keyword in ['faculty', 'school', 'arts', 'education', 'business', 'law', 'health', 'science', 'engineering']):
            continue
            
        # This could be a faculty heading
        faculty_name = heading_text
        description = []
        
        # Get paragraphs following this heading
        next_elem = heading.find_next_sibling()
        while next_elem and len(description) < 3:
            if next_elem.name == 'p':
                text = clean_text(next_elem.get_text())
                if text and len(text) > 30:
                    description.append(text)
            next_elem = next_elem.find_next_sibling()
        
        if description:
            faculties.append({
                'name': faculty_name,
                'description': ' '.join(description)
            })
    
    # If still no faculties found, try looking for sections that might contain faculty info
    if not faculties:
        sections = main_content.select('section, .content-block, .card')
        for section in sections:
            heading = section.select_one('h2, h3, h4')
            if heading:
                heading_text = clean_text(heading.get_text())
                
                if any(keyword in heading_text.lower() for keyword in ['faculty', 'school', 'arts', 'education', 'business', 'law', 'health', 'science', 'engineering']):
                    faculty_name = heading_text
                    description = []
                    
                    paragraphs = section.select('p')
                    for p in paragraphs:
                        text = clean_text(p.get_text())
                        if text and len(text) > 30:
                            description.append(text)
                    
                    if description:
                        faculties.append({
                            'name': faculty_name,
                            'description': ' '.join(description[:3])  # Take first 3 paragraphs
                        })
    
    return faculties

def extract_specific_faculty_info(html, faculty_name):
    """Extract detailed information about a specific faculty"""
    soup = BeautifulSoup(html, 'html.parser')
    description = []
    schools = []
    
    # Find main content area
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        return None
    
    # Extract paragraphs that likely contain faculty description
    paragraphs = main_content.select('p')
    for p in paragraphs:
        text = clean_text(p.get_text())
        if text and len(text) > 30:  # Skip very short texts
            description.append(text)
    
    # Try to find schools or departments within the faculty
    school_sections = main_content.select('.school, .department, .academic-unit')
    if school_sections:
        for section in school_sections:
            school_name = None
            heading = section.select_one('h3, h4, h5, .title')
            if heading:
                school_name = clean_text(heading.get_text())
                schools.append(school_name)
    
    # If no specific schools found, look for lists that might contain schools
    if not schools:
        list_items = main_content.select('ul li, ol li')
        for item in list_items:
            text = clean_text(item.get_text())
            if text and len(text) > 10 and any(word in text.lower() for word in ['school', 'department']):
                schools.append(text)
    
    return {
        'name': faculty_name,
        'description': ' '.join(description[:3]),  # Take first 3 paragraphs for conciseness
        'schools': schools[:5]  # Take up to 5 schools
    }

def extract_services_info(html):
    """Extract information about student services"""
    soup = BeautifulSoup(html, 'html.parser')
    services = []
    
    # Find main content
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        main_content = soup
    
    # Look for service sections in headings and subsequent paragraphs
    service_headings = main_content.select('h2, h3')
    
    for heading in service_headings:
        heading_text = clean_text(heading.get_text())
        
        # Skip if not a service-related heading
        if not any(keyword in heading_text.lower() for keyword in ['support', 'service', 'help', 'wellbeing', 'career', 'counselling', 'library', 'study']):
            continue
            
        # This could be a service heading
        service_name = heading_text
        description = []
        
        # Get paragraphs following this heading
        next_elem = heading.find_next_sibling()
        while next_elem and len(description) < 3:
            if next_elem.name == 'p':
                text = clean_text(next_elem.get_text())
                if text and len(text) > 30:
                    description.append(text)
            next_elem = next_elem.find_next_sibling()
        
        if description:
            services.append({
                'name': service_name,
                'description': ' '.join(description)
            })
    
    # If we didn't find services using the heading approach, try looking for service cards
    if not services:
        service_cards = main_content.select('.card, .grid-item, .feature, .content-block')
        for card in service_cards:
            heading = card.select_one('h2, h3, h4, .title, .heading')
            if heading:
                service_name = clean_text(heading.get_text())
                description = []
                
                paragraphs = card.select('p')
                for p in paragraphs:
                    text = clean_text(p.get_text())
                    if text and len(text) > 30:
                        description.append(text)
                
                if description:
                    services.append({
                        'name': service_name,
                        'description': ' '.join(description[:2])  # Take first 2 paragraphs
                    })
    
    return services

def extract_student_central_info(html):
    """Extract specific information about Student Central"""
    soup = BeautifulSoup(html, 'html.parser')
    description = []
    services = []
    
    # Find main content area
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        return None
    
    # Find sections that mention Student Central
    sections = main_content.find_all(['div', 'section'])
    for section in sections:
        text = section.get_text().lower()
        if 'student central' in text:
            paragraphs = section.select('p')
            for p in paragraphs:
                clean_p = clean_text(p.get_text())
                if clean_p and len(clean_p) > 30:
                    description.append(clean_p)
            break
    
    # Extract services if available
    service_list = main_content.select('ul li, ol li')
    for item in service_list:
        service_text = clean_text(item.get_text())
        if service_text and len(service_text) > 10:
            services.append(service_text)
    
    return {
        'name': 'Student Central',
        'description': ' '.join(description[:3]),
        'services': services[:5]
    }

def extract_online_systems(html):
    """Extract information about online systems and platforms"""
    soup = BeautifulSoup(html, 'html.parser')
    systems = []
    
    # Look for references to online systems
    keywords = [
        'clouddeakin', 'cloud deakin', 'deakinsync', 'studentconnect', 'deakintalent', 
        'student portal', 'learning management system', 'lms', 'digital tools', 'online learning',
        'library', 'student central', 'my course', 'timetable'
    ]
    
    # Find main content
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        main_content = soup
    
    # Look for headings related to online systems
    headings = main_content.select('h2, h3, h4')
    for heading in headings:
        heading_text = clean_text(heading.get_text()).lower()
        
        for keyword in keywords:
            if keyword in heading_text:
                system_name = clean_text(heading.get_text())
                description = []
                
                # Get paragraphs following this heading
                next_elem = heading.find_next_sibling()
                while next_elem and len(description) < 2:
                    if next_elem.name == 'p':
                        text = clean_text(next_elem.get_text())
                        if text and len(text) > 30:
                            description.append(text)
                    next_elem = next_elem.find_next_sibling()
                
                if description:
                    systems.append({
                        'name': system_name,
                        'description': ' '.join(description)
                    })
                break
    
    # Also look for paragraphs that mention the keywords
    paragraphs = main_content.select('p')
    for p in paragraphs:
        p_text = p.get_text().lower()
        for keyword in keywords:
            if keyword in p_text:
                # Get the nearest preceding heading for context
                heading_text = "Deakin Online System"
                for heading in headings:
                    if heading.sourceline < p.sourceline:
                        potential_heading = clean_text(heading.get_text())
                        if potential_heading:
                            heading_text = potential_heading
                
                # Use the paragraph as description
                description = clean_text(p.get_text())
                if description and len(description) > 30:
                    # Check if we already have this system
                    system_exists = False
                    for system in systems:
                        if keyword.title() in system['name'] or heading_text == system['name']:
                            system_exists = True
                            break
                    
                    if not system_exists:
                        systems.append({
                            'name': heading_text,
                            'description': description
                        })
                break
    
    return systems

def extract_specific_online_systems(html, system_name):
    """Extract detailed information about specific online systems like CloudDeakin or OnTrack"""
    soup = BeautifulSoup(html, 'html.parser')
    description = []
    features = []
    
    # Find main content area
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        main_content = soup
    
    # Look for content mentioning the system
    system_lower = system_name.lower()
    
    # Try to find headings that mention the system
    headings = main_content.select('h1, h2, h3, h4')
    for heading in headings:
        heading_text = heading.get_text().lower()
        if system_lower in heading_text:
            # Extract paragraphs following this heading
            elem = heading.next_sibling
            while elem and len(description) < 3:
                if hasattr(elem, 'name') and elem.name == 'p':
                    text = clean_text(elem.get_text())
                    if text and len(text) > 30:
                        description.append(text)
                elif hasattr(elem, 'name') and elem.name in ['div', 'section']:
                    for p in elem.select('p'):
                        text = clean_text(p.get_text())
                        if text and len(text) > 30:
                            description.append(text)
                            if len(description) >= 3:
                                break
                elem = elem.next_sibling
    
    # If no description found from headings, search for paragraphs mentioning the system
    if not description:
        paragraphs = main_content.select('p')
        for p in paragraphs:
            text = p.get_text().lower()
            if system_lower in text:
                clean_p = clean_text(p.get_text())
                if clean_p and len(clean_p) > 30:
                    description.append(clean_p)
                if len(description) >= 3:
                    break
    
    # If still no description, try to look for divs with class descriptions or similar
    if not description:
        div_selectors = main_content.select('.description, .summary, .content-block, .about, .info')
        for div in div_selectors:
            text = div.get_text().lower()
            if system_lower in text:
                # Try to extract paragraphs or text content
                paragraphs = div.select('p')
                if paragraphs:
                    for p in paragraphs:
                        clean_p = clean_text(p.get_text())
                        if clean_p and len(clean_p) > 30:
                            description.append(clean_p)
                        if len(description) >= 3:
                            break
                else:
                    # Try to get the text directly
                    clean_text_content = clean_text(div.get_text())
                    if clean_text_content and len(clean_text_content) > 30:
                        # Split long text into sentences
                        sentences = re.split(r'[.!?]+', clean_text_content)
                        for sentence in sentences:
                            clean_sentence = clean_text(sentence)
                            if clean_sentence and len(clean_sentence) > 30:
                                description.append(clean_sentence)
                            if len(description) >= 3:
                                break
    
    # Look for features in lists near mentions of the system
    list_containers = main_content.find_all(['ul', 'ol'])
    for container in list_containers:
        container_text = container.get_text().lower()
        if system_lower in container_text:
            list_items = container.find_all('li')
            for item in list_items:
                feature_text = clean_text(item.get_text())
                if feature_text and len(feature_text) > 10:
                    features.append(feature_text)
                if len(features) >= 5:
                    break
    
    # If no features found yet, try other list items
    if not features:
        key_features = main_content.select('li')
        for item in key_features[:5]:  # Get up to 5 features
            feature_text = clean_text(item.get_text())
            if feature_text and len(feature_text) > 10:
                features.append(feature_text)
    
    # If we still don't have a description from web scraping, use our predefined description
    if not description and system_name in SYSTEM_DESCRIPTIONS:
        description = [SYSTEM_DESCRIPTIONS[system_name]]
    
    return {
        'name': system_name,
        'description': ' '.join(description[:3]),
        'features': features[:5]
    }

def extract_academic_info(html):
    """Extract information about academic policies and procedures"""
    soup = BeautifulSoup(html, 'html.parser')
    academic_info = {
        'policies': [],
        'procedures': [],
        'resources': []
    }
    
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        main_content = soup
    
    # Extract policies
    policy_sections = main_content.select('.policy, .section, article')
    for section in policy_sections:
        heading = section.select_one('h2, h3, h4')
        if heading:
            policy_text = clean_text(heading.get_text())
            if policy_text and len(policy_text) > 10:
                academic_info['policies'].append(policy_text)
    
    # Extract procedures
    procedure_items = main_content.select('ol li, .procedure-item')
    for item in procedure_items:
        procedure_text = clean_text(item.get_text())
        if procedure_text and len(procedure_text) > 20:
            academic_info['procedures'].append(procedure_text)
    
    # Extract resources
    resource_links = main_content.select('a[href*="resource"], a[href*="guide"]')
    for link in resource_links:
        resource_text = clean_text(link.get_text())
        if resource_text and len(resource_text) > 5:
            academic_info['resources'].append({
                'title': resource_text,
                'url': link.get('href', '')
            })
    
    return academic_info

def extract_student_services(html):
    """Extract information about student services"""
    soup = BeautifulSoup(html, 'html.parser')
    services = {
        'health': [],
        'counselling': [],
        'career': [],
        'clubs': []
    }
    
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        main_content = soup
    
    # Extract health services
    health_sections = main_content.select('.health-service, .medical-service')
    for section in health_sections:
        service_text = clean_text(section.get_text())
        if service_text and len(service_text) > 20:
            services['health'].append(service_text)
    
    # Extract counselling services
    counselling_sections = main_content.select('.counselling-service, .mental-health')
    for section in counselling_sections:
        service_text = clean_text(section.get_text())
        if service_text and len(service_text) > 20:
            services['counselling'].append(service_text)
    
    # Extract career services
    career_sections = main_content.select('.career-service, .employment-service')
    for section in career_sections:
        service_text = clean_text(section.get_text())
        if service_text and len(service_text) > 20:
            services['career'].append(service_text)
    
    # Extract clubs and societies
    club_sections = main_content.select('.club, .society, .student-group')
    for section in club_sections:
        club_text = clean_text(section.get_text())
        if club_text and len(club_text) > 10:
            services['clubs'].append(club_text)
    
    return services

def extract_campus_facilities(html):
    """Extract information about campus facilities"""
    soup = BeautifulSoup(html, 'html.parser')
    facilities = {
        'library': [],
        'sports': [],
        'dining': [],
        'parking': []
    }
    
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        main_content = soup
    
    # Extract library facilities
    library_sections = main_content.select('.library-service, .library-facility')
    for section in library_sections:
        facility_text = clean_text(section.get_text())
        if facility_text and len(facility_text) > 20:
            facilities['library'].append(facility_text)
    
    # Extract sports facilities
    sports_sections = main_content.select('.sports-facility, .fitness-center')
    for section in sports_sections:
        facility_text = clean_text(section.get_text())
        if facility_text and len(facility_text) > 20:
            facilities['sports'].append(facility_text)
    
    # Extract dining facilities
    dining_sections = main_content.select('.dining-facility, .food-service')
    for section in dining_sections:
        facility_text = clean_text(section.get_text())
        if facility_text and len(facility_text) > 20:
            facilities['dining'].append(facility_text)
    
    # Extract parking information
    parking_sections = main_content.select('.parking-info, .parking-facility')
    for section in parking_sections:
        facility_text = clean_text(section.get_text())
        if facility_text and len(facility_text) > 20:
            facilities['parking'].append(facility_text)
    
    return facilities

def extract_international_services(html):
    """Extract information about international student services"""
    soup = BeautifulSoup(html, 'html.parser')
    services = {
        'visa': [],
        'accommodation': [],
        'language': [],
        'cultural': []
    }
    
    main_content = soup.select_one('main, .content, #content')
    if not main_content:
        main_content = soup
    
    # Extract visa information
    visa_sections = main_content.select('.visa-info, .immigration-info')
    for section in visa_sections:
        info_text = clean_text(section.get_text())
        if info_text and len(info_text) > 20:
            services['visa'].append(info_text)
    
    # Extract accommodation information
    accommodation_sections = main_content.select('.accommodation-info, .housing-info')
    for section in accommodation_sections:
        info_text = clean_text(section.get_text())
        if info_text and len(info_text) > 20:
            services['accommodation'].append(info_text)
    
    # Extract language support information
    language_sections = main_content.select('.language-support, .language-service')
    for section in language_sections:
        info_text = clean_text(section.get_text())
        if info_text and len(info_text) > 20:
            services['language'].append(info_text)
    
    # Extract cultural support information
    cultural_sections = main_content.select('.cultural-support, .cultural-service')
    for section in cultural_sections:
        info_text = clean_text(section.get_text())
        if info_text and len(info_text) > 20:
            services['cultural'].append(info_text)
    
    return services

def extract_news_info(html):
    """Extract news and media releases"""
    soup = BeautifulSoup(html, 'html.parser')
    news_items = []
    # Try to find news articles
    articles = soup.select('article, .news-item, .media-release, .card, .card-content')
    for article in articles:
        title = article.select_one('h2, h3, .title, a')
        date = article.select_one('.date, .published-date, time')
        content = article.select_one('.content, .summary, .excerpt, p')
        if title:
            news_items.append({
                'title': clean_text(title.get_text()),
                'date': clean_text(date.get_text()) if date else '',
                'content': clean_text(content.get_text()) if content else ''
            })
    return news_items

def extract_scholarship_info(html):
    """Extract scholarship information"""
    soup = BeautifulSoup(html, 'html.parser')
    scholarships = []
    
    # Find scholarship projects
    items = soup.select('.scholarship-item, .scholarship-card, article')
    for item in items:
        title = item.select_one('h2, h3, .title')
        description = item.select_one('.description, .summary, .content')
        value = item.select_one('.value, .amount')
        
        if title:
            scholarships.append({
                'name': clean_text(title.get_text()),
                'description': clean_text(description.get_text()) if description else '',
                'value': clean_text(value.get_text()) if value else ''
            })
    
    return scholarships

def extract_career_info(html):
    """Extract careers and employment information"""
    soup = BeautifulSoup(html, 'html.parser')
    career_info = []
    # Try to find career sections
    sections = soup.select('.career-section, .service-item, article, .card, .card-content')
    for section in sections:
        title = section.select_one('h2, h3, .title, a')
        description = section.select_one('.description, .content, p')
        services = section.select('ul li, .service-point')
        if title:
            career_info.append({
                'title': clean_text(title.get_text()),
                'description': clean_text(description.get_text()) if description else '',
                'services': [clean_text(service.get_text()) for service in services if service.get_text().strip()]
            })
    return career_info

def extract_course_info(html):
    """Extract information about courses and study areas"""
    soup = BeautifulSoup(html, 'html.parser')
    courses = []
    
    # Find course sections
    course_sections = soup.select('.course-card, .study-area, article')
    for section in course_sections:
        title = section.select_one('h2, h3, .title, a')
        description = section.select_one('.description, .summary, p')
        if title:
            courses.append({
                'title': clean_text(title.get_text()),
                'description': clean_text(description.get_text()) if description else '',
                'url': title.get('href', '') if title.name == 'a' else ''
            })
    
    return courses

def extract_student_services_info(html):
    """Extract information about student services"""
    soup = BeautifulSoup(html, 'html.parser')
    services = []
    
    # Find service sections
    service_sections = soup.select('.service-card, .support-service, article')
    for section in service_sections:
        title = section.select_one('h2, h3, .title, a')
        description = section.select_one('.description, .summary, p')
        if title:
            services.append({
                'title': clean_text(title.get_text()),
                'description': clean_text(description.get_text()) if description else '',
                'url': title.get('href', '') if title.name == 'a' else ''
            })
    
    return services

def extract_campus_life_info(html):
    """Extract information about campus life and events"""
    soup = BeautifulSoup(html, 'html.parser')
    events = []
    
    # Find event sections
    event_sections = soup.select('.event-card, .activity, article')
    for section in event_sections:
        title = section.select_one('h2, h3, .title, a')
        date = section.select_one('.date, time')
        description = section.select_one('.description, .summary, p')
        if title:
            events.append({
                'title': clean_text(title.get_text()),
                'date': clean_text(date.get_text()) if date else '',
                'description': clean_text(description.get_text()) if description else '',
                'url': title.get('href', '') if title.name == 'a' else ''
            })
    
    return events

def generate_markdown(data):
    """Generate markdown document in English"""
    md = "# Deakin University Information\n\n"
    
    # News and Media
    if data.get('news'):
        md += "## News and Media\n\n"
        for item in data['news']:
            md += f"### {item['title']}\n"
            if item['date']:
                md += f"*{item['date']}*\n\n"
            md += f"{item['content']}\n\n"
    
    # Courses and Study
    if data.get('courses'):
        md += "## Courses and Study\n\n"
        for course in data['courses']:
            md += f"### {course['title']}\n"
            if course['description']:
                md += f"{course['description']}\n\n"
            if course['url']:
                md += f"[Learn More]({course['url']})\n\n"
    
    # Careers and Employment
    if data.get('careers'):
        md += "## Careers and Employment\n\n"
        for career in data['careers']:
            md += f"### {career['title']}\n"
            md += f"{career['description']}\n\n"
            if career['services']:
                md += "**Available Services:**\n"
                for service in career['services']:
                    md += f"- {service}\n"
                md += "\n"
    
    # Student Services
    if data.get('student_services'):
        md += "## Student Services\n\n"
        for service in data['student_services']:
            md += f"### {service['title']}\n"
            if service['description']:
                md += f"{service['description']}\n\n"
            if service['url']:
                md += f"[Learn More]({service['url']})\n\n"
    
    # Campus Life
    if data.get('campus_life'):
        md += "## Campus Life\n\n"
        for event in data['campus_life']:
            md += f"### {event['title']}\n"
            if event['date']:
                md += f"*{event['date']}*\n\n"
            if event['description']:
                md += f"{event['description']}\n\n"
            if event['url']:
                md += f"[Learn More]({event['url']})\n\n"
    
    # International Students
    if data.get('international'):
        md += "## International Students\n\n"
        for category, items in data['international'].items():
            if items:
                md += f"### {category.title()}\n"
                for item in items:
                    md += f"- {item}\n"
                md += "\n"
    
    # Research and Innovation
    if data.get('research'):
        md += "## Research and Innovation\n\n"
        for category, items in data['research'].items():
            if items:
                md += f"### {category.title()}\n"
                for item in items:
                    md += f"- {item}\n"
                md += "\n"
    
    # Student Life
    if data.get('student_life'):
        md += "## Student Life\n\n"
        for category, items in data['student_life'].items():
            if items:
                md += f"### {category.title()}\n"
                for item in items:
                    md += f"- {item}\n"
                md += "\n"
    
    # Campus Facilities
    if data.get('facilities'):
        md += "## Campus Facilities\n\n"
        for category, items in data['facilities'].items():
            if items:
                md += f"### {category.title()}\n"
                for item in items:
                    md += f"- {item}\n"
                md += "\n"
    
    # Academic Support
    if data.get('academic'):
        md += "## Academic Support\n\n"
        for category, items in data['academic'].items():
            if items:
                md += f"### {category.title()}\n"
                for item in items:
                    md += f"- {item}\n"
                md += "\n"
    
    # Online Systems
    if data.get('online_systems'):
        md += "## Online Systems\n\n"
        for system in data['online_systems']:
            md += f"### {system['name']}\n"
            md += f"{system['description']}\n\n"
            if system.get('features'):
                md += "**Key Features:**\n"
                for feature in system['features']:
                    md += f"- {feature}\n"
                md += "\n"
    
    # Add timestamp
    md += f"\n---\nLast updated: {time.strftime('%Y-%m-%d %H:%M:%S')}\n"
    return md

def main():
    """Main function to run the scraper"""
    all_data = {
        'news': [],
        'courses': [],
        'careers': [],
        'student_services': [],
        'campus_life': [],
        'international': {},
        'research': {},
        'student_life': {},
        'facilities': {},
        'academic': {},
        'online_systems': []
    }
    
    for url_name, url in URLS.items():
        print(f"\nProcessing: {url_name}")
        html = fetch_url(url)
        if html:
            if 'news' in url_name:
                news_info = extract_news_info(html)
                if news_info:
                    all_data['news'].extend(news_info)
                    print(f"Found {len(news_info)} news entries")
            
            elif 'course' in url_name or 'study' in url_name:
                course_info = extract_course_info(html)
                if course_info:
                    all_data['courses'].extend(course_info)
                    print(f"Found {len(course_info)} course entries")
            
            elif 'career' in url_name:
                career_info = extract_career_info(html)
                if career_info:
                    all_data['careers'].extend(career_info)
                    print(f"Found {len(career_info)} career entries")
            
            elif 'service' in url_name:
                service_info = extract_student_services_info(html)
                if service_info:
                    all_data['student_services'].extend(service_info)
                    print(f"Found {len(service_info)} service entries")
            
            elif 'life' in url_name or 'event' in url_name or 'sport' in url_name:
                life_info = extract_campus_life_info(html)
                if life_info:
                    all_data['campus_life'].extend(life_info)
                    print(f"Found {len(life_info)} campus life entries")
            
            if 'international' in url_name:
                international_info = extract_international_services(html)
                if international_info:
                    for key, value in international_info.items():
                        if value:
                            if key not in all_data['international']:
                                all_data['international'][key] = []
                            all_data['international'][key].extend(value)
                            print(f"Found {len(value)} {key} international service entries")
            if 'research' in url_name:
                research_info = extract_academic_info(html)
                if research_info:
                    for key, value in research_info.items():
                        if value:
                            if key not in all_data['research']:
                                all_data['research'][key] = []
                            all_data['research'][key].extend(value)
                            print(f"Found {len(value)} {key} research entries")
            if 'student_life' in url_name or 'clubs' in url_name:
                student_life_info = extract_student_services(html)
                if student_life_info:
                    for key, value in student_life_info.items():
                        if value:
                            if key not in all_data['student_life']:
                                all_data['student_life'][key] = []
                            all_data['student_life'][key].extend(value)
                            print(f"Found {len(value)} {key} student life entries")
            if 'facility' in url_name or 'campus' in url_name or 'library' in url_name:
                facility_info = extract_campus_facilities(html)
                if facility_info:
                    for key, value in facility_info.items():
                        if value:
                            if key not in all_data['facilities']:
                                all_data['facilities'][key] = []
                            all_data['facilities'][key].extend(value)
                            print(f"Found {len(value)} {key} facility entries")
            if 'study' in url_name or 'academic' in url_name:
                academic_info = extract_academic_info(html)
                if academic_info:
                    for key, value in academic_info.items():
                        if value:
                            if key not in all_data['academic']:
                                all_data['academic'][key] = []
                            all_data['academic'][key].extend(value)
                            print(f"Found {len(value)} {key} academic entries")
            if any(system in url_name.lower() for system in ['clouddeakin', 'deakinsync', 'ontrack', 'studentconnect']):
                system_info = extract_specific_online_systems(html, url_name.split('_')[0].title())
                if system_info:
                    all_data['online_systems'].append(system_info)
                    print(f"Found information for {system_info['name']}")
    
    markdown_content = generate_markdown(all_data)
    with open('deakin_info.md', 'w', encoding='utf-8') as f:
        f.write(markdown_content)
    
    print("\nScraping Summary:")
    print(f"News: {len(all_data['news'])} entries")
    print(f"Courses: {len(all_data['courses'])} entries")
    print(f"Careers: {len(all_data['careers'])} entries")
    print(f"Student Services: {len(all_data['student_services'])} entries")
    print(f"Campus Life: {len(all_data['campus_life'])} entries")
    print(f"Online Systems: {len(all_data['online_systems'])} entries")
    
    print("\nInternational Services:")
    for key, value in all_data['international'].items():
        print(f"- {key}: {len(value)} entries")
    print("\nResearch Information:")
    for key, value in all_data['research'].items():
        print(f"- {key}: {len(value)} entries")
    print("\nStudent Life:")
    for key, value in all_data['student_life'].items():
        print(f"- {key}: {len(value)} entries")
    print("\nFacilities:")
    for key, value in all_data['facilities'].items():
        print(f"- {key}: {len(value)} entries")
    print("\nAcademic Support:")
    for key, value in all_data['academic'].items():
        print(f"- {key}: {len(value)} entries")

if __name__ == "__main__":
    main() 