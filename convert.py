from fpdf import FPDF

class PDF(FPDF):
    def header(self):
        self.set_font('Helvetica', 'B', 10)
        self.set_text_color(150, 150, 150)
        self.cell(0, 10, 'StudySpark - Android Development Setup Guide', align='R')
        self.ln(10)

    def footer(self):
        self.set_y(-15)
        self.set_font('Helvetica', 'I', 8)
        self.set_text_color(150, 150, 150)
        self.cell(0, 10, f'Page {self.page_no()}', align='C')

pdf = PDF()
pdf.add_page()
pdf.set_auto_page_break(auto=True, margin=15)
# Set a safe width that leaves room for margins
safe_width = pdf.w - 20 

with open("guide.md", "r") as f:
    lines = f.readlines()

for line in lines:
    line = line.encode('ascii', 'ignore').decode('ascii')
    line = line.strip()
    
    if not line:
        pdf.ln(5)
        continue

    # Headers
    if line.startswith("# "):
        pdf.set_font("Helvetica", "B", 18)
        pdf.set_text_color(108, 99, 255) 
        pdf.multi_cell(safe_width, 10, line[2:])
        pdf.ln(5)
    elif line.startswith("## "):
        pdf.set_font("Helvetica", "B", 14)
        pdf.set_text_color(74, 66, 212) 
        pdf.multi_cell(safe_width, 8, line[3:])
        pdf.ln(3)
    # Code blocks
    elif line.startswith("```") or line.startswith("    "):
        pdf.set_font("Courier", "", 9)
        pdf.set_fill_color(240, 240, 240)
        pdf.set_text_color(50, 50, 50)
        pdf.multi_cell(safe_width, 5, line, fill=True)
        pdf.ln(1)
    # Bullet points
    elif line.startswith("- ") or line.startswith("* "):
        pdf.set_font("Helvetica", "", 11)
        pdf.set_text_color(0, 0, 0)
        pdf.multi_cell(safe_width, 6, "  - " + line[2:])
    # Standard text
    else:
        pdf.set_font("Helvetica", "", 11)
        pdf.set_text_color(0, 0, 0)
        # Use 'write' instead of 'multi_cell' to avoid the horizontal space error on long words
        pdf.write(6, line)
        pdf.ln(6)

pdf.output("StudySpark_Setup_Guide.pdf")
print("PDF generated successfully: StudySpark_Setup_Guide.pdf")
